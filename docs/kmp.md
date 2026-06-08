# Kotlin Multiplatform

MediatorK is a pure KMP library — all APIs live in `commonMain` and work identically on JVM, Android, and iOS.

---

## Installation

```kotlin
// build.gradle.kts (KMP module)
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.fajrbahr:mediatork:0.1.1")
        }
    }
}
```

No platform-specific artifacts needed. The library publishes:

| Artifact | Target |
|---|---|
| `mediatork` | Root metadata |
| `mediatork-jvm` | JVM / Android |
| `mediatork-iosarm64` | iOS device |
| `mediatork-iossimulatararm64` | iOS Simulator (Apple Silicon) |
| `mediatork-iosx64` | iOS Simulator (Intel) |

---

## Define everything in commonMain

All requests, handlers, and registrars go in `commonMain` — the same code runs on every platform:

```kotlin
// commonMain
data class GetProductQuery(val id: String) : Request<Product>

class GetProductHandler(private val repo: ProductRepository) : RequestHandler<GetProductQuery, Product> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: GetProductQuery,
    ): Product = repo.findById(request.id) ?: error("Not found")
}
```

---

## Platform-specific implementations

Inject platform implementations via `expect`/`actual` or constructor injection:

```kotlin
// commonMain
interface ProductRepository {
    suspend fun findById(id: String): Product?
}

// androidMain / iosMain — provide concrete implementations
```

---

## Creating the mediator

```kotlin
// commonMain — create once, share as singleton
val mediator: Mediator = MediatorFactory.create(
    registrars = listOf(ProductRegistrar(productRepository)),
)
```

On iOS, expose the mediator to Swift via a shared singleton or Koin.

---

## With Koin

See [Koin integration](koin.md) for a complete setup that works across Android and iOS.

---

## Next

→ [Spring Boot](spring.md)
