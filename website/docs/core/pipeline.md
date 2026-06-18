---
id: pipeline
title: Pipeline Behaviors
sidebar_label: Pipeline Behaviors
---

# Pipeline Behaviors

A **pipeline behavior** wraps every request in a decorator chain — cross-cutting concerns like logging, retry, caching,
auth, and timing without touching handler code.

```
send(request)
  └─ Behavior 1 (order=-100, outermost)
       └─ Behavior 2 (order=0)
            └─ Behavior 3 (order=10)
                 └─ Handler
```

Lower `order` = outermost wrapper = runs first on the way in, last on the way out.

---

## Implementing a behavior

```kotlin
class LoggingBehavior : PipelineBehavior {
    override val order = -100 // outermost

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        println("→ ${request::class.simpleName}")
        val result = next(request)           // advance the chain
        println("← ${request::class.simpleName}")
        return result
    }
}
```

Call `next(request)` to continue. Return without calling it to short-circuit.

---

## Selective behaviors — `appliesTo`

Restrict a behavior to specific request types without modifying handler code:

```kotlin
class AuthBehavior(
    private val auth0Enabled: Boolean = false,
) : PipelineBehavior {
    override val order = 10

    override fun appliesTo(request: Request<*>): Boolean =
        request is AuthenticatedRequest   // only runs for authenticated requests

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        if (auth0Enabled) {
            requestContext.getMetaDate<String>("token")
                ?: throw UnauthorizedException()
        }
        return next(request)
    }
}
```

Pass `auth0Enabled = true` when Auth0 is configured in your environment — the behavior skips token validation entirely
when it is `false`.

---

## Disabling a behavior — `isEnabled`

```kotlin
class MetricsBehavior(private val config: AppConfig) : PipelineBehavior {
    override val isEnabled: Boolean get() = config.metricsEnabled
    // ...
}
```

---

## Registering behaviors

```kotlin
val mediator = MediatorFactory.create(
    registrars = listOf(AppRegistrar()),
    pipelineBehaviors = listOf(
        LoggingBehavior(),    // order -100, outermost
        AuthBehavior(),       // order 10
        MetricsBehavior(config),
    ),
)
```

Behaviors are sorted by `order` at dispatch time — registration order doesn't matter.

![MediatorK pipeline behaviors — neon dispatch](../../static/img/mediator-night.png)

---

## Built-in behaviors · `com.fajrbahr.mediatork.pipeline`

MediatorK ships 10+ production-ready behaviors. Import them with `com.fajrbahr.mediatork.pipeline.*`.

| Class | Default order | Description |
|-------|---------------|-------------|
| `LoggingPipelineBehavior` | `-100` | Logs request entry and exit with optional result logging. Accepts any `(String) -> Unit` logger. |
| `ValidationBehavior` | `-50` | Runs `ValidationScope.REQUEST` validators and throws `ValidationException` on failure. From `com.fajrbahr.mediatork.validator`. |
| `AuthorizationPipelineBehavior` | `-10` | Only applies to requests implementing `AuthenticatedRequest`. Throws `UnauthorizedException` to deny access. |
| `CachingPipelineBehavior` | `0` | TTL-based cache with mutex locking. Customizable key function. Public API: `invalidate(key)`, `clear()`, `size()`. |
| `RetryPipelineBehavior` | `0` | Retries the handler up to `maxRetries` times. Configurable `delay` and `retryOn` predicate. |
| `TimeoutPipelineBehavior` | `0` | Cancels the downstream pipeline if it exceeds `timeoutMillis`. |
| `RateLimitPipelineBehavior` | `0` | Sliding-window counter per request type. Throws `RateLimitExceededException` immediately — no queueing. |
| `CircuitBreakerPipelineBehavior` | `0` | CLOSED → OPEN → HALF_OPEN → CLOSED state machine. Configurable `failureThreshold` and `resetTimeoutMs`. Optional `onStateChange` callback. |
| `DeduplicationPipelineBehavior` | `0` | Deduplicates concurrent in-flight requests with the same key. Second caller suspends and awaits the first caller's result. |
| `TransactionPipelineBehavior` | `0` | Wraps the handler in a `TransactionProvider`. Commits on success, rolls back and rethrows on exception. |
| `TimingPipelineBehavior` | `0` | Measures handler execution time. Calls `onTimed(requestName, durationMs)` after each dispatch. |
| `ErrorTrackingPipelineBehavior` | `Int.MAX_VALUE` | Calls `onError(request, throwable)` for every unhandled exception, then rethrows it. |
| `RequestCounterPipelineBehavior` | `0` | Counts dispatches per request type. Public API: `countFor(klass)`, `snapshot()`. |

### TransactionPipelineBehavior

The most common use case — wrap write commands in a transaction and ignore reads:

```kotlin
// Implement TransactionProvider once for your persistence layer
val transactionProvider = object : TransactionProvider {
    override suspend fun <T> withTransaction(block: suspend () -> T): T =
        db.withTransaction { block() }  // Room, Exposed, SQLDelight, etc.
}

// Register in the pipeline
val mediator = MediatorFactory.create(
    registrars = listOf(AppRegistrar()),
    pipelineBehaviors = listOf(
        ValidationBehavior(validators),
        TransactionPipelineBehavior(
            transactionProvider = transactionProvider,
            appliesTo = { it is Request.Unit },  // only write commands, not queries
        ),
    ),
)
```

The transaction commits when the handler returns normally. On any exception, the transaction rolls back and the original exception is rethrown unchanged.

---

## Next

→ [Pre / Post Processors](processors.md)
