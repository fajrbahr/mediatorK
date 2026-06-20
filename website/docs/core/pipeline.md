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

## Pipeline stages

Every `PipelineBehavior` has a `stage` property that controls its absolute position in the chain:

| Stage           | Position           | Typical use                                             |
|-----------------|--------------------|---------------------------------------------------------|
| `Stage.Pre`     | Outermost wrappers | Auth token injection, locale setup, trace-id population |
| `Stage.Default` | Middle *(default)* | Logging, retry, caching, circuit-breaking, timing       |
| `Stage.Post`    | Innermost wrappers | Metrics emission, audit logging, response observation   |

**Stage always wins over `order`.** Every `Stage.Pre` behavior executes before every `Stage.Default` behavior,
regardless of their `order` values. `order` only controls sequencing *within* a stage.

```kotlin
class TraceIdBehavior : PipelineBehavior {
    override val stage = Stage.Pre   // runs before all Default behaviors
    override val order = 0

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        requestContext.put("traceId", generateTraceId())
        return next(request)
    }
}
```

All built-in behaviors use `Stage.Default`. See [Pre / Post Behaviors](processors.md) for a detailed guide.

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

MediatorK ships 6 production-ready behaviors. Import them with `com.fajrbahr.mediatork.pipeline.*`.

| Class                            | Default order   | Description                                                                                                               |
|----------------------------------|-----------------|---------------------------------------------------------------------------------------------------------------------------|
| `LoggingPipelineBehavior`        | `-100`          | Logs request entry and exit with optional result logging. Accepts any `(String) -> Unit` logger.                          |
| `ValidationBehavior`             | `-50`           | Runs registered `RequestValidator`s and throws `ValidationException` on failure. From `com.fajrbahr.mediatork.validator`. |
| `CachingPipelineBehavior`        | `0`             | TTL-based cache with mutex locking. Customizable key function. Public API: `invalidate(key)`, `clear()`, `size()`.        |
| `TimeoutPipelineBehavior`        | `0`             | Cancels the downstream pipeline if it exceeds `timeoutMillis`.                                                            |
| `TimingPipelineBehavior`         | `0`             | Measures handler execution time. Calls `onTiming(requestName, durationMs)` after each dispatch.                           |
| `ErrorTrackingPipelineBehavior`  | `Int.MAX_VALUE` | Calls `onError(request, throwable)` for every unhandled exception, then rethrows it.                                      |
| `RequestCounterPipelineBehavior` | `0`             | Counts dispatches per request type. Public API: `countFor(klass)`, `snapshot()`.                                          |

---

## Next

→ [Pre / Post Processors](processors.md)
