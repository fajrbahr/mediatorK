---
id: free-aop
title: Free AOP
sidebar_label: Free AOP
---

# Free AOP

**Aspect-Oriented Programming (AOP)** separates cross-cutting concerns — logging, auth, caching, metrics — from business
logic without modifying the core code.

MediatorK gives you AOP for free through pipeline behaviors. Every behavior you register applies to **all** handlers
automatically. Your handler code stays completely untouched.

---

## The idea

Without AOP, you reach for logging inside every handler:

```kotlin
class GetUserHandler(private val db: UserRepository) : RequestHandler<GetUserQuery, User> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: GetUserQuery
    ): User {
        println("→ GetUserQuery(id=${request.id})")   // ← you added this
        val user = db.findById(request.id) ?: error("not found")
        println("← GetUserQuery result=$user")        // ← and this
        return user
    }
}
```

With AOP, the handler stays pure and logging lives in one place:

```kotlin
// Handler — zero logging code
class GetUserHandler(private val db: UserRepository) : RequestHandler<GetUserQuery, User> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: GetUserQuery
    ): User = db.findById(request.id) ?: error("not found")
}

// Wiring — one line enables logging for every request
val mediator = MediatorFactory.create(
    registrars = listOf(AppRegistrar()),
    pipelineBehaviors = listOf(
        LoggingPipelineBehavior(logger = ::println),
    ),
)
```

Output for every request dispatched:
```
→ GetUserQuery
← GetUserQuery
```

---

## Add logging without touching production code

The full pattern: define handlers normally, then drop `LoggingPipelineBehavior` into the factory at the wiring site.

```kotlin
// 1. Pure handler — no logging imports, no println, no side-effects
class PlaceOrderHandler(
    private val orders: OrderRepository,
    private val events: EventBus,
) : RequestHandler<PlaceOrderCommand, OrderId> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: PlaceOrderCommand
    ): OrderId {
        val id = orders.save(request.toOrder())
        events.publish(OrderPlacedEvent(id))
        return id
    }
}

// 2. Wire up — logging added in one place, covers every handler
val mediator = MediatorFactory.create(
    registrars = listOf(AppRegistrar()),
    pipelineBehaviors = listOf(
        LoggingPipelineBehavior(logger = ::println, logResult = true),
    ),
)
```

Output when `PlaceOrderCommand` is dispatched:
```
→ PlaceOrderCommand
← PlaceOrderCommand result=OrderId(value=abc-123)
```

Ten handlers, one `LoggingPipelineBehavior`. Add a new handler tomorrow — it gets logged too, automatically.

---

## Platform loggers

Swap the logger function to match your runtime — the handler code never changes.

```kotlin
// KMP common
LoggingPipelineBehavior(logger = ::println)

// JVM — SLF4J
val log = LoggerFactory.getLogger("Mediator")
LoggingPipelineBehavior(logger = log::info)

// Android — Logcat
LoggingPipelineBehavior(logger = { msg -> Log.d("Mediator", msg) })

// JS / browser — console
LoggingPipelineBehavior(logger = { msg -> console.log(msg) })
```

---

## Stack multiple concerns

Each behavior is an independent aspect. Combine them freely:

```kotlin
val mediator = MediatorFactory.create(
    registrars = listOf(AppRegistrar()),
    pipelineBehaviors = listOf(
        LoggingPipelineBehavior(logger = ::println,           order = -100), // outermost
        TimingPipelineBehavior(onTiming = { name, ms ->
            println("$name took ${ms}ms")
        }),
        ErrorTrackingPipelineBehavior { request, error ->
            Sentry.captureException(error)
        },
    ),
)
```

None of these touch a single handler. The handler is pure business logic — the pipeline is pure infrastructure.

---

## Selective aspects — `appliesTo`

Target a behavior at a subset of requests without touching handler code:

```kotlin
class AuditLogBehavior(
    private val audit: AuditLogger,
) : PipelineBehavior {
    override val order = 0

    // only applies to commands — queries are skipped automatically
    override fun appliesTo(request: Request<*>): Boolean = request is Command<*>

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        val result = next(request)
        audit.record(request, result)
        return result
    }
}
```

Command handlers get audit logging. Query handlers are unaffected. Zero handler changes either way.

---

## The payoff

| Concern           | Where it lives     | Handler changes |
|-------------------|--------------------|-----------------|
| Logging           | `LoggingPipelineBehavior`        | None |
| Timing / metrics  | `TimingPipelineBehavior`         | None |
| Crash reporting   | `ErrorTrackingPipelineBehavior`  | None |
| Request counting  | `RequestCounterPipelineBehavior` | None |
| Caching           | `CachingPipelineBehavior`        | None |
| Timeout           | `TimeoutPipelineBehavior`        | None |
| Auth / JWT        | Custom `PipelineBehavior`        | None |
| Audit trail       | Custom `PipelineBehavior`        | None |

Cross-cutting concerns live in one place and apply to all handlers automatically.

---

## Next

→ [Pre / Post Processors](processors.md)
