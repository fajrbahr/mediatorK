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
class AuthBehavior : PipelineBehavior {
    override val order = 10

    override fun appliesTo(request: Request<*>): Boolean =
        request is AuthenticatedRequest   // only runs for authenticated requests

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        val token = requestContext.getMetaDate<String>("token")
            ?: throw UnauthorizedException()
        return next(request)
    }
}
```

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

---

## Next

→ [Pre / Post Processors](processors.md)
