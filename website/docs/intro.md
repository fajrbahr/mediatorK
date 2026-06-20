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

## Why MediatorK?

| Feature                     | Description                                                                                            |
|-----------------------------|--------------------------------------------------------------------------------------------------------|
| ⚡ **Coroutine-native**      | `suspend` all the way down — no callbacks, no blocking                                                 |
| 🧩 **KMP ready**            | Works on JVM, Android, iOS, Linux, Windows, WASM, and more from one codebase                           |
| 🌊 **Streaming**            | `StreamRequest<T>` returns a cold `Flow<T>` — lazy, incremental, cancellable                           |
| 🔌 **Framework-agnostic**   | Works with Spring Boot, Ktor, KMP, or plain Kotlin                                                     |
| 🪶 **Zero magic**           | No kotlin-reflect, no code generation, no annotation processors                                        |
| 🛡️ **Built-in validation** | Lightweight `RequestValidator` DSL with `rules { }` and `rulesFailFast { }` — no annotation processing |
| 🔧 **Batteries included**   | 6 built-in pipeline behaviors: caching, logging, timeout, timing, error tracking, and more              |
| 🧪 **Testable by design**   | `buildHandlerTestHarness`, `FakeMediator`, `MediatorSpy` — no mocking library needed                   |

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

| Platform   | Targets                                                                                                 |
|------------|---------------------------------------------------------------------------------------------------------|
| JVM        | `jvm`                                                                                                   |
| Android    | `androidTarget` · `androidNativeArm32` · `androidNativeArm64` · `androidNativeX64` · `androidNativeX86` |
| iOS        | `iosArm64` · `iosSimulatorArm64` · `iosX64`                                                             |
| macOS      | `macosArm64` · `macosX64`                                                                               |
| tvOS       | `tvosArm64` · `tvosSimulatorArm64` · `tvosX64`                                                          |
| watchOS    | `watchosArm32` · `watchosArm64` · `watchosDeviceArm64` · `watchosSimulatorArm64` · `watchosX64`         |
| Linux      | `linuxArm64` · `linuxX64`                                                                               |
| Windows    | `mingwX64`                                                                                              |
| Web / Wasm | `js` · `wasmJs` · `wasmWasi`                                                                            |

Ready to start? [See the promise MediatorK makes →](the-promise.md)
