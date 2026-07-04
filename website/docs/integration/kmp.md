---
id: kmp
title: Kotlin Multiplatform
sidebar_label: Kotlin Multiplatform
---

# Kotlin Multiplatform

MediatorK is a pure KMP library; all APIs live in `commonMain` and work identically on JVM, Android, and iOS.

See [Installation](../installation.md) for dependency coordinates. Add to `commonMain`; Gradle resolves the right
platform artifact automatically.

---

## Define everything in commonMain

All requests, handlers, and registrars go in `commonMain`; the same code runs on every platform:

```kotlin
// commonMain
data class GetProductQuery(val id: String) : Request<Product>

fun HandlerRegistry.productHandlers(repo: ProductRepository) = scope {
    handle<GetProductQuery> { request ->
        repo.findById(request.id) ?: error("Not found")
    }
}
```

---

## Creating the mediator

```kotlin
// commonMain — create once, share as singleton
val mediator: Mediator = MediatorFactory.create(
    registrars = listOf(MediatorRegistrar { it.productHandlers(productRepository) }),
)
```

On iOS, expose the mediator to Swift via a shared singleton or Koin.

---

## With Koin

See [Koin integration](koin.md) for a complete setup that works across Android and iOS.

---

## Next

→ [Ktor](ktor.md)
