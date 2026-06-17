---
id: built-in-behaviors
title: Built-in Pipeline Behaviors
sidebar_label: Built-in Behaviors
---

# Built-in Pipeline Behaviors

MediatorK ships twelve ready-to-use pipeline behaviors. Each one is a plain class you instantiate and pass to
`MediatorFactory.create` — no registration magic, no annotation processing.

---

## Quick reference

| Behavior | Default `order` | What it does |
|---|---|---|
| `LoggingPipelineBehavior` | `-100` | Logs `→ Request` and `← Request` around every handler |
| `AuthorizationPipelineBehavior` | `-10` | Runs an auth check for requests that implement `AuthenticatedRequest` |
| `ValidationBehavior` | `-50` | Runs `RequestValidator` instances and throws on failure |
| `RetryPipelineBehavior` | `0` | Retries the downstream pipeline on failure |
| `TimingPipelineBehavior` | `0` | Reports handler duration via a callback |
| `TimeoutPipelineBehavior` | `0` | Cancels requests that exceed a deadline |
| `RequestCounterPipelineBehavior` | `0` | Counts dispatches per request type |
| `CachingPipelineBehavior` | `0` | Caches handler results for a configurable TTL |
| `DeduplicationPipelineBehavior` | `0` | Coalesces concurrent identical requests into one pipeline run |
| `CircuitBreakerPipelineBehavior` | `0` | Opens the circuit after N consecutive failures |
| `RateLimitPipelineBehavior` | `0` | Enforces a max dispatch rate per request type |
| `ErrorTrackingPipelineBehavior` | `Int.MAX_VALUE` | Forwards unhandled exceptions to a crash-reporting callback |

Lower `order` = outermost wrapper = runs first on the way in, last on the way out.

---

## Logging

`LoggingPipelineBehavior` logs each request as it enters and exits the pipeline. Accepts any `(String) -> Unit` logger
so it works on every platform.

```kotlin
// KMP / plain Kotlin
LoggingPipelineBehavior(logger = ::println)

// Android — Logcat
LoggingPipelineBehavior(logger = { msg -> Log.d("Mediator", msg) })

// JVM — SLF4J
val log = LoggerFactory.getLogger("Mediator")
LoggingPipelineBehavior(logger = log::info)
```

| Parameter | Default | Description |
|---|---|---|
| `logger` | `::println` | Receives each log line |
| `logResult` | `false` | When `true`, appends the result's `toString()` to the exit line |
| `order` | `-100` | Runs outermost by default |

---

## Authorization

`AuthorizationPipelineBehavior` runs a suspend authorization check for every request that implements `AuthenticatedRequest`.
Requests that do not implement the marker interface pass through untouched.

```kotlin
// Mark requests that need auth
data class GetOrderQuery(val id: String) : Request<Order>, AuthenticatedRequest
data class PublicStatusQuery(val id: String) : Request<Status> // no marker — skipped

// Register the behavior
AuthorizationPipelineBehavior { context, request ->
    val token = context.getMetaDate<String>("token")
        ?: throw UnauthorizedException("No token in context")
    if (!tokenValidator.isValid(token)) throw UnauthorizedException("Invalid token")
}
```

| Parameter | Default | Description |
|---|---|---|
| `authorize` | — | Suspend lambda; throw to deny, return to allow |
| `order` | `-10` | After logging, before business-logic behaviors |

Populate `RequestContext` with the token from a pre-processor or earlier behavior — see [Request Context](context.md).

---

## Validation

`ValidationBehavior` wires your `RequestValidator` instances into the pipeline. See [Validation](validation.md) for
the full guide.

```kotlin
ValidationBehavior(
    validators = listOf(CreateTodoValidator(), UpdateTodoValidator()),
)
```

Default order is `-50`. Throws `ValidationException` on failure.

---

## Retry

`RetryPipelineBehavior` retries the entire downstream pipeline (including the handler) when it throws.

```kotlin
RetryPipelineBehavior(
    maxRetries = 3,
    delayMillis = 200,
    retryOn = { it is IOException },
)
```

| Parameter | Default | Description |
|---|---|---|
| `maxRetries` | `3` | Retry attempts after the first failure (total = `maxRetries + 1`) |
| `delayMillis` | `0` | Milliseconds to wait between attempts |
| `retryOn` | `{ true }` | Predicate — return `false` to stop retrying for a specific exception |
| `order` | `0` | |

Place `RetryPipelineBehavior` at a lower `order` than `TimeoutPipelineBehavior` so each retry gets a fresh timeout
budget.

---

## Timing

`TimingPipelineBehavior` measures handler execution time and reports it via a callback. Timing is always reported —
even when the handler throws — so you get latency data for both successful and failed requests.

```kotlin
// KMP / plain Kotlin
TimingPipelineBehavior { name, ms -> println("$name took ${ms}ms") }

// Android — Firebase Performance
TimingPipelineBehavior { name, ms ->
    FirebasePerformance.getInstance().newTrace(name).also { it.start(); it.stop() }
}

// JVM — Micrometer
TimingPipelineBehavior { name, ms ->
    meterRegistry.timer(name).record(ms, TimeUnit.MILLISECONDS)
}
```

| Parameter | Default | Description |
|---|---|---|
| `onTiming` | — | `(requestName: String, durationMs: Long) -> Unit` |
| `order` | `0` | |

---

## Timeout

`TimeoutPipelineBehavior` cancels the downstream pipeline if it does not complete within the deadline.

```kotlin
TimeoutPipelineBehavior(timeoutMillis = 5_000) // cancel after 5 s
```

Throws `TimeoutCancellationException` (a `CancellationException`) when the deadline is exceeded. Pair with
`RetryPipelineBehavior` at a lower `order` to retry timed-out requests.

| Parameter | Default | Description |
|---|---|---|
| `timeoutMillis` | — | Maximum allowed duration in milliseconds (must be > 0) |
| `order` | `0` | |

---

## Request counter

`RequestCounterPipelineBehavior` counts how many times each request type has been dispatched. Counts accumulate for the
lifetime of the behavior instance.

```kotlin
val counter = RequestCounterPipelineBehavior()

val mediator = MediatorFactory.create(
    registrars = listOf(AppRegistrar()),
    pipelineBehaviors = listOf(counter),
)

mediator.send(GetUserQuery("u-1"))
mediator.send(GetUserQuery("u-2"))
mediator.send(CreateOrderCommand(...))

counter.countFor(GetUserQuery::class)      // 2
counter.countFor(CreateOrderCommand::class) // 1
counter.snapshot()                          // map of all request names → counts
```

Call `counter.reset()` to clear all counts. Thread-safe.

---

## Caching

`CachingPipelineBehavior` caches handler results by a request key for a configurable TTL. On a cache hit the handler is
skipped entirely. Best suited for query requests whose results change infrequently.

```kotlin
CachingPipelineBehavior(
    ttlMs = 30_000,                         // cache for 30 seconds
    keyFor = { req -> req.toString() },     // default — full toString
)
```

| Parameter | Default | Description |
|---|---|---|
| `ttlMs` | `60_000` | Time-to-live in milliseconds |
| `keyFor` | `{ it.toString() }` | Produces the cache key from a request |
| `order` | `0` | |

Additional methods: `invalidate(key)`, `clear()`, `size()`. Thread-safe.

Commands that produce side effects should not be cached — only use this for idempotent queries.

---

## Deduplication

`DeduplicationPipelineBehavior` coalesces concurrent in-flight requests that share the same key. When two `send` calls
arrive with the same key while the first is still executing, the second suspends and awaits the first call's result
instead of running a second pipeline.

```kotlin
DeduplicationPipelineBehavior(
    keyFor = { req -> "${req::class.simpleName}:${req}" }
)
```

The default key is the request class name — which deduplicates all concurrent calls of the same type regardless of
field values. Override `keyFor` to include field values (e.g. an ID).

| Parameter | Default | Description |
|---|---|---|
| `keyFor` | `{ it::class.simpleName }` | Deduplication key function |
| `order` | `0` | |

---

## Circuit breaker

`CircuitBreakerPipelineBehavior` implements the circuit-breaker resilience pattern.

After `failureThreshold` consecutive failures the circuit trips to `OPEN` and subsequent requests are rejected
immediately with `CircuitOpenException`. After `resetTimeoutMs` milliseconds it transitions to `HALF_OPEN` and allows
one probe request through — success closes the circuit, failure re-opens it.

```
CLOSED ──(threshold reached)──► OPEN
  ▲                               │
  └── probe success ◄─ HALF_OPEN ◄┘
                          │
                    probe failure
                          │
                        re-OPEN
```

```kotlin
CircuitBreakerPipelineBehavior(
    failureThreshold = 5,
    resetTimeoutMs = 10_000,
    onStateChange = { state -> logger.info("Circuit → $state") },
)
```

| Parameter | Default | Description |
|---|---|---|
| `failureThreshold` | `5` | Consecutive failures before tripping (must be ≥ 1) |
| `resetTimeoutMs` | `10_000` | Time to stay OPEN before probing (must be > 0) |
| `onStateChange` | `null` | Optional callback invoked on every state transition |
| `order` | `0` | |

A single instance tracks state for all request types. Create one instance per service boundary if you need isolated
breakers.

---

## Rate limiting

`RateLimitPipelineBehavior` enforces a maximum dispatch rate per request type using a sliding-window counter. Requests
that exceed the limit throw `RateLimitExceededException` immediately — they are never queued.

```kotlin
RateLimitPipelineBehavior(maxRequests = 5, windowMs = 1_000) // 5 req/s per type
```

| Parameter | Default | Description |
|---|---|---|
| `maxRequests` | — | Max allowed dispatches per `windowMs` (must be ≥ 1) |
| `windowMs` | — | Sliding window duration in milliseconds (must be > 0) |
| `order` | `0` | |

---

## Error tracking

`ErrorTrackingPipelineBehavior` intercepts every unhandled exception and forwards it to a callback before rethrowing.
Use it to wire crash-reporting services (Firebase Crashlytics, Sentry, Bugsnag) without touching handler code.

```kotlin
// Android — Firebase Crashlytics
ErrorTrackingPipelineBehavior { request, error ->
    FirebaseCrashlytics.getInstance().recordException(error)
}

// KMP / Sentry
ErrorTrackingPipelineBehavior { request, error ->
    Sentry.captureException(error)
}
```

| Parameter | Default | Description |
|---|---|---|
| `onError` | — | `(request, throwable) -> Unit` — always called before rethrow |
| `order` | `Int.MAX_VALUE` | Innermost by default — fires after retry/timeout have given up |

---

## Recommended order table

A typical production setup that uses several behaviors together:

| Order | Behavior | Why here |
|---|---|---|
| `−200` | `RetryPipelineBehavior` | Outermost — retries the entire pipeline including timeout |
| `−100` | `LoggingPipelineBehavior` | Logs each attempt |
| `−50` | `ValidationBehavior` | Fail fast before any I/O |
| `−10` | `AuthorizationPipelineBehavior` | Deny early, before business logic |
| `0` | `TimingPipelineBehavior` | Measures handler time |
| `10` | `TimeoutPipelineBehavior` | Cancels per-attempt |
| `20` | `RequestCounterPipelineBehavior` | Increments counter on each attempt |
| `Int.MAX_VALUE` | `ErrorTrackingPipelineBehavior` | Captures final unhandled errors |

---

## Next

→ [Pre / Post Processors](processors.md)
