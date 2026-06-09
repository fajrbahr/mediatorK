# MediatorK

[![Docs](https://img.shields.io/badge/Docs-mediatorK-a97cf8)](https://fajrbahr.github.io/mediatorK/)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.fajrbahr/mediatork?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.fajrbahr/mediatork)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.21-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Gradle](https://img.shields.io/badge/Gradle-9.3.0-02303A.svg?logo=gradle)](https://gradle.org)
[![Android](https://img.shields.io/badge/Android-supported-brightgreen.svg?logo=android)](https://developer.android.com)
[![iOS](https://img.shields.io/badge/iOS-supported-brightgreen.svg?logo=apple)](https://developer.apple.com)
[![Linux](https://img.shields.io/badge/Linux-supported-brightgreen.svg?logo=linux)](https://www.linux.org)
[![Windows](https://img.shields.io/badge/Windows-supported-brightgreen.svg?logo=windows)](https://www.microsoft.com/windows)
[![Web (JS/WASM)](https://img.shields.io/badge/Web%20(JS%2FWASM)-supported-brightgreen.svg?logo=javascript)](https://kotlinlang.org/docs/js-overview.html)
[![CI](https://github.com/fajrbahr/mediatorK/actions/workflows/release.yml/badge.svg)](https://github.com/fajrbahr/mediatorK/actions/workflows/release.yml)
[![License: CC0](https://img.shields.io/badge/License-CC0-brightgreen)](LICENSE)

A coroutine-first Mediator library for Kotlin. Implements the CQRS and Vertical Slice patterns — requests go to exactly
one handler, notifications fan out to many, and a pipeline of behaviors sits in between.

---

## Installation

MediatorK is a Kotlin Multiplatform library. Pick the snippet that matches your project type.

---

### Kotlin JVM (Spring Boot, Ktor, CLI, etc.)

**Gradle Kotlin DSL**

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.fajrbahr:mediatork:0.1.2")
}
```

**Gradle Groovy**

```groovy
// build.gradle
dependencies {
    implementation 'io.github.fajrbahr:mediatork:0.1.2'
}
```

**Maven** — use the `-jvm` artifact ID, Maven does not resolve KMP metadata

```xml

<dependency>
    <groupId>io.github.fajrbahr</groupId>
    <artifactId>mediatork-jvm</artifactId>
    <version>0.1.2</version>
</dependency>
```

---

### Android (single-platform)

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("io.github.fajrbahr:mediatork:0.1.2")
}
```

---

### Kotlin Multiplatform (KMP)

Add to `commonMain` in your shared module — Gradle picks the right platform artifact automatically.

```kotlin
// shared/build.gradle.kts
kotlin {
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation("io.github.fajrbahr:mediatork:0.1.2")
        }
    }
}
```

**Supported targets**

| Target              | Notes                         |
|---------------------|-------------------------------|
| `jvm`               | JVM / Spring Boot / Ktor      |
| `androidTarget`     | Android apps and libraries    |
| `iosArm64`          | iOS device                    |
| `iosSimulatorArm64` | iOS simulator (Apple Silicon) |
| `iosX64`            | iOS simulator (Intel)         |

---

## Core Concepts

| Concept                   | Description                                                 |
|---------------------------|-------------------------------------------------------------|
| `Request<TResponse>`      | A command or query with exactly one handler                 |
| `Notification`            | An event broadcast to zero or more handlers                 |
| `RequestHandler`          | Handles one request type, returns a result                  |
| `NotificationHandler`     | Reacts to one notification type, no return value            |
| `PipelineBehavior`        | Middleware that wraps every request (logging, retry, auth…) |
| `RequestPreProcessor`     | Runs before the handler (validation, enrichment…)           |
| `RequestPostProcessor`    | Runs after the handler (metrics, audit…)                    |
| `RequestExceptionHandler` | Catches exceptions thrown by a specific request             |
| `RequestContext`          | Per-request key/value bag (locale, user, trace ID…)         |

---

## Quick Start

### 1 — Define a Request

```kotlin
// Command with a result
data class CreateOrderCommand(val id: String, val amount: Double) : Request<Order>

// Query
data class GetOrderQuery(val id: String) : Request<Order>

// Command with no result
data class DeleteOrderCommand(val id: String) : Request<Unit>
```

### 2 — Implement a Handler

```kotlin
class CreateOrderHandler(private val db: OrderRepository) : RequestHandler<CreateOrderCommand, Order> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: CreateOrderCommand
    ): Order {
        val order = Order(request.id, request.amount)
        db.save(order)

        // handlers can publish further notifications or send requests
        mediator.publish(OrderCreatedEvent(order.id))

        return order
    }
}
```

### 3 — Define a Notification

```kotlin
data class OrderCreatedEvent(val orderId: String) : Notification
```

### 4 — Implement Notification Handlers

```kotlin
class SendConfirmationEmailHandler : NotificationHandler<OrderCreatedEvent> {
    override suspend fun handle(notification: OrderCreatedEvent) {
        emailService.send(notification.orderId)
    }
}

class UpdateInventoryHandler : NotificationHandler<OrderCreatedEvent> {
    override suspend fun handle(notification: OrderCreatedEvent) {
        inventory.decrement(notification.orderId)
    }
}
```

### 5 — Register Handlers

```kotlin
class OrderRegistrar(private val db: OrderRepository) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.scope {
            +CreateOrderHandler(db)
            +GetOrderHandler(db)
            +DeleteOrderHandler(db)
        }
    }
}

class OrderEventsRegistrar : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.scope {
            +SendConfirmationEmailHandler()
            +UpdateInventoryHandler()
        }
    }
}
```

> `+Handler()` inside `scope { }` auto-detects whether it is a `RequestHandler` or `NotificationHandler` and registers
> it correctly.

### 6 — Create the Mediator

```kotlin
val mediator = MediatorFactory.create(
    registrars = listOf(OrderRegistrar(db), OrderEventsRegistrar()),
)
```

### 7 — Use It

```kotlin
val order = mediator.send(CreateOrderCommand("ORD-1", 150.0))
mediator.publish(OrderCreatedEvent("ORD-1"))
```

---

## MediatorFactory

```kotlin
val mediator = MediatorFactory.create(
    registrars = listOf(OrderRegistrar(), UserRegistrar()),
    pipelineBehaviors = listOf(LoggingBehavior(), RetryBehavior()),
    preProcessors = listOf(AuthPreProcessor()),
    postProcessors = listOf(MetricsPostProcessor()),
    notificationPublisher = ParallelNotificationPublisher()   // default
)
```

All parameters are optional and default to empty lists / `ParallelNotificationPublisher`.

---

## Pipeline Behaviors

Behaviors wrap every request in order. Implement `PipelineBehavior`:

```kotlin
class LoggingBehavior : PipelineBehavior {
    override val order: Int = 0   // lower = outer

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        println("-> ${request::class.simpleName}")
        val result = next(request)
        println("<- ${request::class.simpleName}")
        return result
    }
}
```

**Optional overrides:**

| Property             | Default | Purpose                                             |
|----------------------|---------|-----------------------------------------------------|
| `order`              | `0`     | Execution order; lower runs first (outermost)       |
| `isEnabled`          | `true`  | Toggle without removing from the list               |
| `appliesTo(request)` | `true`  | Filter which request types this behavior applies to |

**Retry example:**

```kotlin
class RetryBehavior(private val maxRetries: Int = 3) : PipelineBehavior {
    override val order = 10

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        repeat(maxRetries - 1) {
            try {
                return next(request)
            } catch (_: Exception) {
            }
        }
        return next(request)
    }
}
```

---

## Pre / Post Processors

Run before and after the handler, outside of pipeline behaviors.

```kotlin
class AuthPreProcessor : RequestPreProcessor {
    override val order = 0
    override suspend fun process(requestContext: RequestContext, request: Request<*>) {
        requestContext.put("userId", currentUser.id)
    }
}

class MetricsPostProcessor : RequestPostProcessor {
    override val order = 0
    override suspend fun process(requestContext: RequestContext, request: Request<*>, response: Any?) {
        metrics.record(request::class.simpleName!!)
    }
}
```

---

## RequestContext

A per-request key/value bag scoped to a single pipeline execution. Safe under concurrent requests — each `send()` call
gets its own isolated instance.

```kotlin
// Write (typically in a pre-processor or behavior)
requestContext.put("locale", "en")
requestContext.put("userId", "u-123")

// Read (in a handler or post-processor)
val locale = requestContext.getMetaDate<String>("locale")
val userId = requestContext.getMetaDate<String>("userId")
```

---

## Exception Handling

Register a typed exception handler per request type:

```kotlin
class CreateOrderExceptionHandler
    : RequestExceptionHandler<CreateOrderCommand, Order, IllegalArgumentException> {

    override suspend fun handle(
        requestContext: RequestContext,
        request: CreateOrderCommand,
        exception: IllegalArgumentException,
    ): Order {
        // return a fallback value or rethrow
        throw exception
    }
}

// Register
registry.registerExceptionHandler(
    CreateOrderCommand::class,
    IllegalArgumentException::class,
    CreateOrderExceptionHandler()
)
```

---

## Notification Publishers

Choose how notification handlers are invoked by passing a publisher to `MediatorFactory.create()`.

| Publisher                                   | Behaviour                                                                            |
|---------------------------------------------|--------------------------------------------------------------------------------------|
| `ParallelNotificationPublisher` *(default)* | All handlers run concurrently via `coroutineScope`                                   |
| `SequentialNotificationPublisher`           | Handlers run one by one; stops on first exception                                    |
| `ContinueOnExceptionNotificationPublisher`  | All handlers run even if some fail; errors collected into `AggregateException`       |
| `FireAndForgetNotificationPublisher(scope)` | Returns immediately; handlers run in the background on the provided `CoroutineScope` |

Override per call:

```kotlin
mediator.publish(OrderCreatedEvent("ORD-1"), SequentialNotificationPublisher())
```

---

## Validation

MediatorK ships a lightweight validation DSL.

### `rules { }` — collect all errors

```kotlin
val result = rules {
    check(request.amount > 0) { "Amount must be positive" }
    ruleFor(OrderField.Id, request.id) {
        check(it.isNotBlank()) { "Order ID is required" }
        check(it.length <= 50) { "Order ID too long" }
    }
}

if (!result.isValid) throw ValidationException(result)
```

### `rulesFailFast { }` — stop at first error

```kotlin
val result = rulesFailFast {
    check(request.id.isNotBlank()) { "ID required" }
    check(request.amount > 0) { "Amount must be positive" }
}
```

### `ValidationResult` helpers

```kotlin
ValidationResult.Success
ValidationResult.error("Something went wrong")
ValidationResult.error(OrderField.Amount, "Must be positive")
ValidationResult.failure(error1, error2)
```

### Custom field markers

```kotlin
enum class OrderField : FieldValidator { Id, Amount, CustomerId }

ruleFor(OrderField.Amount, request.amount) {
    check(it > 0) { "Must be greater than 0" }
}
```

---

## DI Integration

### Manual

```kotlin
val mediator = MediatorFactory.create(
    registrars = listOf(OrderRegistrar(OrderRepository()))
)
```

### Koin

```kotlin
val appModule = module {
    single { OrderRepository() }
    single<Mediator> {
        MediatorFactory.create(registrars = listOf(OrderRegistrar(get())))
    }
}
```

### Spring Boot (WebFlux + coroutines)

```kotlin
@Configuration
class MediatorConfig(private val db: OrderRepository) {
    @Bean
    fun mediator(): Mediator = MediatorFactory.create(
        registrars = listOf(OrderRegistrar(db))
    )
}

@Service
class OrderService(private val mediator: Mediator) {
    suspend fun create(id: String, amount: Double): Order =
        mediator.send(CreateOrderCommand(id, amount))
}
```

> For `suspend` support in Spring, use **Spring WebFlux** with `kotlinx-coroutines-reactor`.

---

## Testing

Swap real handlers for fakes — no mocking library needed:

```kotlin
@Test
fun `create order returns order`() = runTest {
        val mediator = MediatorFactory.create(
            registrars = listOf(object : MediatorRegistrar {
                override fun register(registry: HandlerRegistry) {
                    registry register FakeCreateOrderHandler()
                }
            })
        )

        val order = mediator.send(CreateOrderCommand("ORD-1", 99.0))
        assertEquals("ORD-1", order.id)
    }
```

---

## API Reference

### `Mediator`

```kotlin
suspend fun <TRequest : Request<TResult>, TResult> send(request: TRequest): TResult
suspend fun <T : Notification> publish(notification: T)
suspend fun <T : Notification> publish(notification: T, publisher: NotificationPublisher)
```

### `HandlerRegistry`

```kotlin
infix fun <TRequest, TResult> register(handler: RequestHandler<TRequest, TResult>): HandlerRegistry
infix fun <T : Notification> registerNotification(handler: NotificationHandler<T>): HandlerRegistry
fun <TRequest, TResult, TEx : Throwable> registerExceptionHandler(...): HandlerRegistry
fun scope(block: HandlerRegistry.() -> Unit)
operator fun RequestHandler<*, *>.unaryPlus()
operator fun NotificationHandler<*>.unaryPlus()
fun hasHandler(requestType: KClass<*>): Boolean
```

### `MediatorFactory`

```kotlin
fun create(
    registrars: List<MediatorRegistrar> = emptyList(),
    pipelineBehaviors: List<PipelineBehavior> = emptyList(),
    preProcessors: List<RequestPreProcessor> = emptyList(),
    postProcessors: List<RequestPostProcessor> = emptyList(),
    notificationPublisher: NotificationPublisher = ParallelNotificationPublisher(),
): Mediator
```

---

## Exceptions

| Exception                 | Thrown when                                                                      |
|---------------------------|----------------------------------------------------------------------------------|
| `MissingHandlerException` | `send()` is called with no handler registered for that request type              |
| `AggregateException`      | `ContinueOnExceptionNotificationPublisher` collects one or more handler failures |

---

## License

Released under the [CC0 1.0 Universal](LICENSE) — public domain. No attribution required.

---

## Acknowledgements

First and above all — **الحمد لله** (Alhamdulillah). This library was built during a hard time, and every line was
written with Allah's help and guidance.

**[Jimmy Bogard](https://www.jimmybogard.com/)** — for his talks on Vertical Slice Architecture and MediatR (.NET),
which were the direct inspiration for bringing this pattern to Kotlin.

**[beno.com](https://beno.com)** — the production environment that shaped this library. Real-world usage at scale drove
every design decision here.

**Ahmed Akilan** — our CTO, whose technical mentorship and trust made it possible to grow as an engineer and ship
something worth sharing.

---

## Inspired by MediatR (.NET)

MediatorK is inspired by [MediatR](https://github.com/jbogard/MediatR) by Jimmy Bogard, the most widely-used mediator
library in the .NET ecosystem.
