---
id: free-aop
title: Free AOP
sidebar_label: Free AOP
---

# Free AOP

**Aspect-Oriented Programming (AOP)** separates cross-cutting concerns (logging, auth, caching, metrics) from business
logic without modifying the core code.

MediatorK gives you AOP for free through pipeline behaviors. Every behavior you register applies to **all** handlers
automatically. Your handler code stays completely untouched.

Command handlers get audit logging. Query handlers are unaffected. Zero handler changes either way.

Cross-cutting concerns live in one place and apply to all handlers automatically. None of these touch a single handler. The handler is pure business logic; the pipeline is pure infrastructure.

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

None of these touch a single handler. The handler is pure business logic; the pipeline is pure infrastructure.

---

## Next

→ [Pre / Post Processors](processors.md)
