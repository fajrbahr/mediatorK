---
id: intro
title: Introduction
sidebar_label: Introduction
slug: /intro
---

# Introduction

**MediatorK** is a coroutine-first Mediator library for Kotlin and Kotlin Multiplatform.

It implements the **CQRS** and **Vertical Slice** patterns:

- Every **request** is routed to exactly one handler
- Every **notification** fans out to many handlers
- A **pipeline** of behaviors sits in between (logging, caching, timeout, validation…)

No kotlin-reflect. No annotation processing. No framework required.

---

---

## Quick Example

```kotlin
// 1. Define a request
data class GetUserQuery(val id: String) : Request<User>

// 2. Implement a handler
class GetUserHandler(private val db: UserRepository) : RequestHandler<GetUserQuery, User> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: GetUserQuery
    ): User = db.findById(request.id) ?: error("User not found")
}

// 3. Wire it up
val mediator = MediatorFactory.create(
    registrars = listOf(object : MediatorRegistrar {
        override fun register(registry: HandlerRegistry) {
            registry register GetUserHandler(db)
        }
    })
)

// 4. Use it
val user = mediator.send(GetUserQuery("user-1"))
```

Ready to start? [See the promise MediatorK makes →](the-promise.mdx)
