---
id: resilience
title: Resilience Patterns
sidebar_label: Resilience Patterns
---

# Resilience Patterns

Because every request flows through the [pipeline](pipeline.md), resilience patterns — retry, circuit breaking,
graceful degradation — are just `PipelineBehavior` implementations. Handlers stay clean; resilience is configured once
at `MediatorFactory.create`.

This page provides three complete, copy-paste-ready behaviors and the ordering rules for composing them.

---

## Retry with exponential backoff

Re-invokes the downstream chain when it throws, doubling the delay between attempts. The final attempt propagates its
exception unchanged.

```kotlin
class RetryPipelineBehavior(
    private val maxAttempts: Int = 3,
    private val initialDelayMillis: Long = 200,
    private val backoffFactor: Double = 2.0,
    private val maxDelayMillis: Long = 5_000,
    private val retryOn: (Throwable) -> Boolean = { true },
    override val order: Int = 50,
) : PipelineBehavior {

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        var delayMillis = initialDelayMillis
        repeat(maxAttempts - 1) { attempt ->
            try {
                return next(request)
            } catch (e: CancellationException) {
                throw e                          // never swallow coroutine cancellation
            } catch (e: Throwable) {
                if (!retryOn(e)) throw e
                requestContext.put("retry.attempt", attempt + 1)
                delay(delayMillis)
                delayMillis = (delayMillis * backoffFactor).toLong().coerceAtMost(maxDelayMillis)
            }
        }
        return next(request)                     // final attempt — failure propagates
    }
}
```

Register with a predicate that only retries transient failures:

```kotlin
RetryPipelineBehavior(
    maxAttempts = 3,
    retryOn = { it is TransientNetworkException },
)
```

:::danger Retries require idempotency
A retried `CreateOrderCommand` that failed *after* the order was written creates a duplicate order. Only retry
handlers that are idempotent (safe to run twice), or restrict the behavior to read-only queries with `appliesTo`:

```kotlin
override fun appliesTo(request: Request<*>): Boolean = request is Query<*>
```

Never retry `ValidationException`, `MissingHandlerException`, or other deterministic failures — the second attempt
fails identically, just later. Encode that in `retryOn`.
:::

---

## Circuit breaker

After `failureThreshold` consecutive failures the circuit **opens** and every dispatch fails fast with
`CircuitOpenException` — no handler call, no waiting on a dependency that is already down. After `openDuration`, one
probe call is allowed through (**half-open**); success closes the circuit, failure re-opens it.

```kotlin
class CircuitOpenException(requestName: String) :
    Exception("Circuit is open for '$requestName' — failing fast")

class CircuitBreakerPipelineBehavior(
    private val failureThreshold: Int = 5,
    private val openDuration: Duration = 30.seconds,
    override val order: Int = 60,
) : PipelineBehavior {

    private enum class State { CLOSED, OPEN, HALF_OPEN }

    private val mutex = Mutex()
    private var state = State.CLOSED
    private var failureCount = 0
    private var openedAt: TimeMark? = null

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        mutex.withLock {
            if (state == State.OPEN) {
                val elapsed = openedAt?.elapsedNow() ?: Duration.ZERO
                if (elapsed < openDuration) {
                    throw CircuitOpenException(request::class.simpleName ?: "Unknown")
                }
                state = State.HALF_OPEN          // allow one probe call through
            }
        }

        return try {
            val result = next(request)
            mutex.withLock {
                state = State.CLOSED
                failureCount = 0
                openedAt = null
            }
            result
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            mutex.withLock {
                failureCount++
                if (state == State.HALF_OPEN || failureCount >= failureThreshold) {
                    state = State.OPEN
                    openedAt = TimeSource.Monotonic.markNow()
                }
            }
            throw e
        }
    }
}
```

Uses `kotlin.time.TimeSource.Monotonic` and `kotlinx.coroutines.sync.Mutex`, so it works in `commonMain` on every
target.

:::info One breaker instance = one circuit
State lives in the behavior instance, so a single registered breaker is shared by **every request type it applies
to** — failures in `GetInvoicesQuery` would open the circuit for `GetUsersQuery` too. Add a
`handles: (Request<*>) -> Boolean` constructor parameter, override `appliesTo(request) = handles(request)`, and
register one instance per backend:

```kotlin
CircuitBreakerPipelineBehavior(handles = { it is PaymentsRequest }),   // payments circuit
CircuitBreakerPipelineBehavior(handles = { it is CatalogRequest }),    // catalog circuit
```

:::

---

## Graceful degradation (fallback on error)

When the primary path fails, serve a degraded response — cached data, an empty list, a placeholder — instead of an
exception. Because `process` is generic, a reusable fallback behavior takes an untyped producer and casts:

```kotlin
class FallbackBehavior(
    private val handles: (Request<*>) -> Boolean,
    private val fallback: suspend (request: Request<*>, error: Throwable) -> Any?,
    override val order: Int = 30,
) : PipelineBehavior {

    override fun appliesTo(request: Request<*>): Boolean = handles(request)

    @Suppress("UNCHECKED_CAST")
    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult = try {
        next(request)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        fallback(request, e) as TResult
    }
}
```

Usage — an unavailable recommendations service degrades to an empty list:

```kotlin
FallbackBehavior(
    handles = { it is GetRecommendationsQuery },
    fallback = { _, error ->
        logger.warn("Recommendations degraded: ${error.message}")
        emptyList<Recommendation>()
    },
)
```

The cast is unchecked — the fallback **must** return the request's result type, which is why `handles` should pin the
behavior to specific request types. For per-type, compile-time-safe fallback chains, prefer the
[`otherwise` handler chains](fallback.md) instead; use this behavior when the degraded value is generic (cache lookup,
`null`, empty collection) across a family of requests.

---

## Composing the three: stage and order

All three belong in `Stage.Default` (the interface default). Within the stage, **lower `order` = outermost**, so:

```kotlin
val mediator = MediatorFactory.create(
    registrars = listOf(AppRegistrar()),
    pipelineBehaviors = listOf(
        FallbackBehavior(handles = { it is GetRecommendationsQuery }, fallback = ::degraded), // 30 — outermost
        RetryPipelineBehavior(retryOn = { it is TransientNetworkException }),                 // 50
        CircuitBreakerPipelineBehavior(),                                                     // 60 — innermost
    ),
)
```

```
Fallback (order 30)
  └─ Retry (order 50)
       └─ Circuit breaker (order 60)
            └─ Handler
```

Why this order:

- **Breaker innermost** — every retry attempt passes through the breaker, so an open circuit fails each attempt fast
  instead of hammering a dead dependency. Make sure `retryOn` excludes `CircuitOpenException`, otherwise retry burns
  its attempts (and backoff delays) against an open circuit.
- **Retry in the middle** — it sees raw handler failures and the breaker's fail-fast, but never masks the fallback.
- **Fallback outermost** — it catches whatever survives retry and the breaker, including `CircuitOpenException`, and
  turns the final failure into a degraded response.

Keep resilience behaviors out of `Stage.Pre` (reserved for context population — auth, locale, trace ids) and
`Stage.Post` (response observation). [Stage always beats order](pipeline.md#pipeline-stages), so a retry behavior
accidentally placed in `Stage.Pre` would wrap *outside* your logging and error tracking no matter what order you give
it.

---

## Caveats

- **Requests only.** Pipeline behaviors wrap `send()` dispatches. Notifications published via `publish()` do **not**
  pass through the pipeline — resilience for notification handlers comes from the
  [publish strategy](notifications.md) (`CONTINUE_ON_EXCEPTION` collects failures into an `AggregateException`;
  `fireAndForget` detaches them entirely). Stream requests likewise need
  [stream pipeline behaviors](stream-behaviors.md) — e.g. `Flow.retry { }` inside one.
- **Timeouts compose with retry.** Register the built-in `TimeoutPipelineBehavior` with an order *higher* than retry
  (inside it) for a per-attempt timeout, or *lower* (outside it) for a total budget across all attempts.
- **Error tracking placement.** `ErrorTrackingPipelineBehavior` defaults to `Int.MAX_VALUE` (innermost) and will
  report every individual retry attempt. Give it an order lower than the retry behavior to report only failures that
  exhausted all retries — see [Exception Handling](exceptions.md#errortrackingpipelinebehavior).
- **Always rethrow `CancellationException`.** All three examples do; if you write your own behavior, a swallowed
  cancellation breaks structured concurrency.

---

## Next

→ [Validation](validation.md)
