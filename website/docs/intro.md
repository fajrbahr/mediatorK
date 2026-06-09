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
- A **pipeline** of behaviors sits in between (logging, retry, auth, validation…)

No kotlin-reflect. No annotation processing. No framework required.

---

## Why MediatorK?

| Feature | Description |
|---|---|
| ⚡ **Coroutine-native** | `suspend` all the way down — no callbacks, no blocking |
| 🧩 **KMP ready** | Works on JVM, Android, and iOS from a single `commonMain` dependency |
| 🔌 **Framework-agnostic** | Works with Spring Boot, Ktor, KMM, or plain Kotlin |
| 🪶 **Zero magic** | No kotlin-reflect, no code generation, no annotation processors |
| 🧪 **Testable by design** | Swap real handlers for fakes — no mocking library needed |

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

---

## Supported Targets

| Platform | Target |
|---|---|
| JVM / Spring Boot / Ktor | `jvm` |
| Android | `androidTarget` |
| iOS Device | `iosArm64` |
| iOS Simulator (Apple Silicon) | `iosSimulatorArm64` |
| iOS Simulator (Intel) | `iosX64` |

Ready to start? Head to [Installation →](installation.md)
