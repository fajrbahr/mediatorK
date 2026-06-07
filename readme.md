# Mediatork – The Kotlin Multiplatform Mediator

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-blue.svg)](https://kotlinlang.org)
[![KMP](https://img.shields.io/badge/Kotlin%20Multiplatform-supported-purple)](https://kotlinlang.org/docs/multiplatform.html)
[![Coroutines](https://img.shields.io/badge/Coroutines-1.8+-green)](https://kotlinlang.org/docs/coroutines-overview.html)
[![CC0 1.0 Universal](https://img.shields.io/badge/License-CC0-brightgreen)](LICENSE)

**Mediatork** brings the proven MediatR patterns to **Kotlin** – and goes beyond by being **100% multiplatform**. Build maintainable, scalable applications with CQRS and Vertical Slice Architecture across JVM, Android, iOS, JS, Wasm, and native platforms.

---

## 🚀 Example Usage

```kotlin
val order = mediator.send(CreateOrderCommand("ORD-1", 150.0))
```

```kotlin
mediator.publish(OrderCreatedNotification("ORD-1", "customer@mail.com"))
```

---

## ⭐ Why Mediatork?

* ✅ Kotlin-first (`suspend` everywhere)
* ✅ Fully multiplatform (JVM, Android, iOS, JS, Wasm, Native)
* ✅ Apache 2.0 (free forever)
* ✅ Built-in `RequestContext` (locale, user, tracing)
* ✅ Pipeline system (middleware-style)
* ✅ 4 notification strategies out of the box

### Notification Publishers

* `ParallelNotificationPublisher` (default, concurrent)
* `SequentialNotificationPublisher` (fail fast)
* `ContinueOnExceptionNotificationPublisher` (collect errors)
* `FireAndForgetNotificationPublisher` (background)

---

## 🧩 Core Capabilities

* **Request/Response (CQRS)** – exactly one handler per request
* **Notifications (Events)** – zero to many handlers
* **PipelineBehavior** – middleware layer
* **Pre/Post Processors**
* **Exception Handling**
* **RequestContext (scoped state)**

---

## 📦 Installation

```kotlin
implementation("com.fajrbahr:mediatork:1.0.0")
```

---

## ⚡ Quick Start

### 1. Define Request

```kotlin
data class CreateOrderCommand(val id: String, val amount: Double) : Request<OrderResult>
```

### 2. Define Handler

```kotlin
class CreateOrderHandler : RequestHandler<CreateOrderCommand, OrderResult> {
    override suspend fun handle(ctx: RequestContext, req: CreateOrderCommand): OrderResult {
        return OrderResult(success = true)
    }
}
```

### 3. Define Notification

```kotlin
data class OrderCreatedNotification(val orderId: String, val customerEmail: String) : Notification
```

### 4. Define Notification Handlers

```kotlin
class SendConfirmationEmailHandler : NotificationHandler<OrderCreatedNotification>
class UpdateInventoryHandler : NotificationHandler<OrderCreatedNotification>
```

### 5. Register Everything

```kotlin
class MyRegistrar : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.register(CreateOrderCommand::class, CreateOrderHandler())
        registry.registerNotification(OrderCreatedNotification::class, SendConfirmationEmailHandler())
        registry.registerNotification(OrderCreatedNotification::class, UpdateInventoryHandler())
    }
}
```

### 6. Build Mediator

```kotlin
val mediator = MediatorFactory.create(
    registrars = listOf(MyRegistrar()),
    pipelineBehaviors = listOf(),
    preProcessors = listOf(),
    postProcessors = listOf(),
    notificationPublisher = ParallelNotificationPublisher()
)
```

---

## 🔧 Advanced Features

### RequestContext Example

```kotlin
ctx.locale = Locale.getDefault().language
println(ctx.locale)
```

### Retry Behavior

```kotlin
class RetryBehavior : PipelineBehavior {
    // retry logic with backoff
}
```

### Custom Notification Publisher

```kotlin
withTimeout(1000) {
    // run handlers
}
```

---

## 🧪 Testability

```kotlin
val registry = HandlerRegistry().apply {
    register(MyCommand::class, MockHandler())
}
```

---

## 🌍 Multiplatform Support

* JVM (8+)
* Android
* iOS (arm64, x64)
* JS (IR & legacy)
* Wasm (WasmJS, WasmWasi)
* Windows (mingwX64)
* macOS (arm64, x64)
* Linux (x64, arm64)

---
## ⚙️ Full Mediator Setup Example

```kotlin
val mediator = MediatorFactory.create(
    registrars = listOf(
        UserRegistrar(),
        OrderRegistrar(),
        OrderNotificationRegistrar()
    ),
    pipelineBehaviors = listOf(
        LoggingBehavior(),
        RetryPipelineBehavior(maxRetries = 2),
        TracingPipelineBehavior()
    ),
    preProcessors = listOf(
        AuthPreProcessor(),
        LocalePreProcessor()
    ),
    postProcessors = listOf(
        MetricsPostProcessor()
    ),
    notificationPublisher = ParallelNotificationPublisher()
)
```

---

## 📦 Registrars

### 🧾 OrderRegistrar

Registers request/response handlers:

```kotlin
class OrderRegistrar(
    private val fakeApi: FakeApi = FakeApi()
) : MediatorRegistrar {

    override fun register(registry: HandlerRegistry) {
        registry.scope {
            +CreateOrderHandler(api = fakeApi)
        }
    }
}
```

---

### 📣 OrderNotificationRegistrar

Registers notification (event) handlers:

```kotlin
class OrderNotificationRegistrar : MediatorRegistrar {

    override fun register(registry: HandlerRegistry) {
        registry.scope {
            +SendOrderConfirmationEmailHandler()
            +SendOrderSmsHandler()
            +TrackOrderAnalyticsHandler()

            // explicit style also supported
            registerNotification(UpdateInventoryHandler())
        }
    }
}
```

---

## 💡 Notes

* `registry.scope { +Handler() }` → idiomatic, clean DSL-style registration
* `+Handler()` automatically detects and registers the correct handler type
* You can mix DSL (`+`) and explicit (`registerNotification(...)`) styles
* `ParallelNotificationPublisher` runs all notification handlers concurrently

---

## 🔌 DI Integration

Mediatork works seamlessly with any dependency injection approach: **Spring Boot**, **Koin**, or even **manual DI**.

---

### 🌱 Spring Boot

Define a `Mediator` bean and auto-register handlers:

```kotlin
@Configuration
class MediatorConfig {

    @Bean
    fun mediator(handlers: List<RequestHandler<*, *>>): Mediator {
        val registry = HandlerRegistry()

        // Auto-register all discovered handlers
        handlers.forEach { handler ->
            @Suppress("UNCHECKED_CAST")
            registry.register(handler)
        }

        return MediatorFactory.create(
            registrars = listOf(RegistryRegistrar(registry))
        )
    }
}

class RegistryRegistrar(
    private val registry: HandlerRegistry
) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        // already registered
    }
}
```

Use `Mediator` inside your services or controllers:

```kotlin
@Service
class UserService(private val mediator: Mediator) {

    suspend fun findUser(id: Long): UserDto {
        return mediator.send(GetUserByIdQuery(id))
    }
}
```

```kotlin
data class GetUserByIdQuery(val id: Long) : Request<UserDto>
```

```kotlin
@Component
class GetUserByIdQueryHandler(
    private val userRepository: UserRepository
) : RequestHandler<GetUserByIdQuery, UserDto> {

    override suspend fun handle(
        ctx: RequestContext,
        query: GetUserByIdQuery
    ): UserDto {
        val user = userRepository.findById(query.id)
        return UserDto(user.id, user.name)
    }
}
```

> ⚠️ For `suspend` support, use **Spring WebFlux** or add `kotlinx-coroutines-reactor`.

---

### 🧩 Koin

```kotlin
val mediatorModule = module {

    single { createMediator(get()) }

    single { GetUserByIdQueryHandler(get()) }
}

private fun createMediator(koin: Koin): Mediator {
    val registry = HandlerRegistry()

    // Register handlers from Koin
    registry.register(koin.get<GetUserByIdQueryHandler>())

    return MediatorFactory.create(
        registrars = listOf(RegistryRegistrar(registry))
    )
}
```

Inject and use anywhere:

```kotlin
class UserService(private val mediator: Mediator) {

    suspend fun findUser(id: Long): UserDto {
        return mediator.send(GetUserByIdQuery(id))
    }
}
```

---

### 🧱 Manual DI (Simple)

```kotlin
val registry = HandlerRegistry().apply {
    register(CreateOrderHandler(FakeApi()))
}

val mediator = MediatorFactory.create(
    registrars = listOf(RegistryRegistrar(registry))
)
```

---


## 📜 License

This project is released under the CC0 1.0 Universal (Public Domain Dedication).

You are free to:

Use it in personal, open-source, and commercial projects
Modify and distribute it
Copy the repository fully or partially

No attribution
No restrictions
Full reuse without credit

Use it however you like.


## 🤝 Contributing

Contributions are welcome!


## Inspired by MediatR (.NET)
MediatR is the most popular mediator library in the .NET ecosystem, created by Jimmy Bogard.
MediatR is widely used in CQRS and Clean Architecture, helping developers build scalable and maintainable applications by reducing direct dependencies between components.