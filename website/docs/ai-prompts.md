---
id: ai-prompts
title: AI Guide & Prompts
sidebar_label: For AI/Claude
---

# MediatorK for AI Systems

This guide teaches AI systems (Claude, ChatGPT, Copilot, etc.) how to use MediatorK and includes ready-to-use prompts for code generation.

---

## What is MediatorK?

**MediatorK** is a Kotlin library implementing **CQRS (Command Query Responsibility Segregation)** and **Vertical Slice Architecture** patterns.

- **Single Responsibility**: One request → one handler (no fan-out)
- **Notifications**: One event → many notification handlers (fan-out)
- **Coroutine-first**: All handlers are `suspend fun` for async/await
- **Kotlin Multiplatform**: Runs on Android, iOS, JVM, JS, Native

### Core Concepts

| Concept | Purpose | Example |
|---------|---------|---------|
| **Request** | A message asking for work | `CreateOrderCommand(id, amount)` |
| **Handler** | Code that executes a request | `CreateOrderHandler: RequestHandler<CreateOrderCommand, Order>` |
| **Notification** | An event published after work | `OrderCreatedEvent(orderId)` |
| **Registrar** | Groups handlers together | `OrderRegistrar: MediatorRegistrar` |
| **Mediator** | Central dispatcher | `mediator.send(CreateOrderCommand(...))` |

---

## Core Patterns

### Pattern 1: Request & Handler (0.8.1+)

```kotlin
// 1. Define the Request
data class CreateOrderCommand(
    val orderId: String,
    val amount: Double
) : Request<Order>

// 2. Implement the Handler
class CreateOrderHandler(
    private val orderRepository: OrderRepository
) : RequestHandler<CreateOrderCommand, Order> {
    
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: CreateOrderCommand,
    ): Order {
        val order = Order(request.orderId, request.amount)
        orderRepository.save(order)
        mediator.publish(OrderCreatedEvent(order.orderId))
        return order
    }
}

// 3. Register it
class OrderRegistrar(
    private val orderRepository: OrderRepository
) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry register CreateOrderHandler(orderRepository)
    }
}

// 4. Use it
val mediator = MediatorFactory.create(registrars = listOf(OrderRegistrar(repo)))
val order = mediator.send(CreateOrderCommand("ORD-1", 150.0))
```

### Pattern 2: Notifications

```kotlin
// 1. Define the Notification
data class OrderCreatedEvent(val orderId: String) : Notification

// 2. Implement handlers (multiple can exist)
class SendConfirmationEmailHandler : NotificationHandler<OrderCreatedEvent> {
    override suspend fun handle(notification: OrderCreatedEvent) {
        emailService.send(notification.orderId, "Order confirmed!")
    }
}

class LogOrderCreatedHandler : NotificationHandler<OrderCreatedEvent> {
    override suspend fun handle(notification: OrderCreatedEvent) {
        logger.info("Order created: ${notification.orderId}")
    }
}

// 3. Register them
class OrderRegistrar(...) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry register CreateOrderHandler(...)
        registry registerNotification SendConfirmationEmailHandler()
        registry registerNotification LogOrderCreatedHandler()
    }
}

// 4. Both handlers execute when event is published
mediator.publish(OrderCreatedEvent("ORD-1"))
```

### Pattern 3: Validation

```kotlin
// Create a Validator
class CreateOrderValidator : RequestValidator<CreateOrderCommand> {
    override fun validate(request: CreateOrderCommand): ValidationResult = rules {
        check(request.amount > 0) { "Amount must be positive" }
        check(request.orderId.isNotBlank()) { "Order ID required" }
    }
}

// Register it
class OrderRegistrar(...) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry register CreateOrderHandler(...)
        registry.registerValidator(CreateOrderValidator())
    }
}

// Validation runs automatically before handler
mediator.send(CreateOrderCommand("", -100))
// → Throws ValidationException with all errors
```

### Pattern 4: Vertical Slice Architecture

```
src/main/kotlin/com/example/app/
  orders/                        ← Feature: Orders (self-contained slice)
    Order.kt                      ← Model
    CreateOrderCommand.kt         ← Request
    CreateOrderHandler.kt         ← Handler
    CreateOrderValidator.kt       ← Validation
    OrderRegistrar.kt             ← Wiring
    OrderRepository.kt            ← Data access
    
  payments/                       ← Feature: Payments (independent slice)
    Payment.kt
    ProcessPaymentCommand.kt
    ProcessPaymentHandler.kt
    PaymentRegistrar.kt
    PaymentRepository.kt

  Application.kt                  ← Wire all registrars
```

Each feature is **self-contained** — can be added/removed independently.

---

## Common Mistakes to Avoid

| ❌ Wrong | ✅ Right |
|---------|---------|
| `registry register MyRequest()` | `registry register MyHandler()` |
| `class MyClass { fun handle() }` | `class MyHandler : RequestHandler { }` |
| Blocking I/O: `URL(...).readText()` | Suspend I/O: `httpClient.get(...)` |
| `override fun handle()` | `override suspend fun handle()` |
| Shared request types across features | Each feature owns its own requests |

---

## API Reference

```kotlin
// Request Interface
interface Request<TResult>

// Handler Interface  
interface RequestHandler<TRequest : Request<TResult>, TResult> {
    suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: TRequest,
    ): TResult
}

// Notification
interface Notification
interface NotificationHandler<TNotification : Notification> {
    suspend fun handle(notification: TNotification)
}

// Validator
interface RequestValidator<TRequest : Request<*>> {
    fun validate(request: TRequest): ValidationResult
}

// Registrar
interface MediatorRegistrar {
    fun register(registry: HandlerRegistry)
}

// Create Mediator
val mediator = MediatorFactory.create(
    registrars = listOf(OrderRegistrar(...), PaymentRegistrar(...))
)

// Send Request
val result = mediator.send(CreateOrderCommand(...))

// Publish Notification
mediator.publish(OrderCreatedEvent(...))
```

---

## Samples to Reference

Point users to these working examples:

- **[Basic](https://github.com/fajrbahr/MediatorK/tree/main/samples/basic)** — Todo app
- **[Android](https://github.com/fajrbahr/MediatorK/tree/main/samples/sample-university)** — University domain
- **[Ktor](https://github.com/fajrbahr/MediatorK/tree/main/samples/sample-ktor)** — HTTP API (vertical slices)
- **[Spring Boot](https://github.com/fajrbahr/MediatorK/tree/main/samples/sample-spring)** — REST API (vertical slices)
- **[JVM](https://github.com/fajrbahr/MediatorK/tree/main/samples/sample)** — Full-featured app

---

# Ready-to-Use Prompts

Copy any of these and paste into Claude, ChatGPT, or Copilot. Replace `[...]` with your specific requirements.

---

## Prompt 1: Create a Request & Handler

```
Using MediatorK 0.8.1 for Kotlin, create a request class and handler for the following:

[describe what it does, e.g. "fetch a list of products from the database"]

Requirements:
- Request should be a data class implementing Request<T>
- Handler should implement RequestHandler<TRequest, TResponse>
- Use constructor injection for dependencies
- Use suspend functions
- Include all imports
- Handler should be registered in a Registrar class
```

**Example Usage:**
```
User: "Using MediatorK, create a request and handler for fetching users by ID from a repository"
Claude: [generates GetUserQuery, GetUserHandler, UserRegistrar with complete imports]
```

---

## Prompt 2: Create a Command with Handler

```
Using MediatorK 0.8.1, create a command and handler for:

[describe the action, e.g. "delete a user account by ID from the database and publish a UserDeletedEvent"]

Requirements:
- Request implements Request<Unit> (no return value)
- Handler implements RequestHandler<TRequest, Unit>
- Publish an appropriate Notification after the action
- Use constructor injection
- Include all imports
- Show the Registrar registration
```

**Example Usage:**
```
User: "Create a DeleteUserCommand handler that deletes a user and publishes a UserDeletedEvent"
Claude: [generates DeleteUserCommand, DeleteUserHandler, registration with event publishing]
```

---

## Prompt 3: Create Notifications & Multiple Handlers

```
Using MediatorK 0.8.1, create a notification class and multiple handlers for:

[describe the event, e.g. "user signed up — send welcome email, log event, and add to newsletter"]

Requirements:
- Notification implements Notification
- Create one handler for each responsibility
- Each handler implements NotificationHandler<TNotification>
- Handlers are independent (each handles one concern)
- Include all imports
- Show how to register all handlers in a Registrar
```

**Example Usage:**
```
User: "When a UserSignedUpEvent occurs, I need to send welcome email, log it, and add to mailing list"
Claude: [generates UserSignedUpEvent, SendWelcomeEmailHandler, LogSignupHandler, AddToMailingListHandler]
```

---

## Prompt 4: Add Validation to a Request

```
Using MediatorK 0.8.1, create a RequestValidator for:

[describe validation rules, e.g. "CreateOrderCommand must have a non-blank cartId, positive amount, and valid email"]

Requirements:
- Create a class implementing RequestValidator<CreateOrderCommand>
- Use the rules { } builder with check() functions
- Each failed check should have a clear error message
- Register the validator in the Registrar
- Include all imports
```

**Example Usage:**
```
User: "Add validation to CreateOrderCommand: amount > 0, cartId not blank, customerEmail is valid"
Claude: [generates CreateOrderValidator with all validation rules and registration]
```

---

## Prompt 5: Create a Pipeline Behavior

```
Using MediatorK 0.8.1, create a pipeline behavior that:

[describe the cross-cutting concern, e.g. "logs the request type and execution time for every request"]

Requirements:
- Implement PipelineBehavior<TRequest, TResponse>
- Use suspend function and call next()
- Work generically for all request types
- Include configuration options if needed (e.g., threshold for slow requests)
- Show how to register it in MediatorFactory.create()
- Include all imports
```

**Example Usage:**
```
User: "Create a logging pipeline behavior that logs request name and execution time"
Claude: [generates LoggingBehavior with suspend process() and next() call]
```

---

## Prompt 6: Create an Android ViewModel

```
Using MediatorK 0.8.1 with Android ViewModel, create a ViewModel for:

[describe the screen, e.g. "product list screen that loads products on init and supports pull-to-refresh"]

Requirements:
- Inject Mediator via constructor (Hilt or manual DI)
- Send requests using mediator.send()
- Expose state via StateFlow or LiveData
- Handle loading and error states
- Use viewModelScope.launch for coroutines
- Include all imports
- Handle cancellation properly
```

**Example Usage:**
```
User: "Create a ProductListViewModel that loads products on init, handles loading/error states, and supports refresh"
Claude: [generates ViewModel with StateFlow, loading states, error handling]
```

---

## Prompt 7: Create a Spring Boot Controller

```
Using MediatorK 0.8.1 with Spring Boot, create a REST controller that:

[describe the endpoint, e.g. "POST /orders to create an order and return the created order"]

Requirements:
- Inject Mediator via Spring constructor injection
- Use suspend functions with Spring WebFlux
- Send requests using mediator.send()
- Return appropriate HTTP status codes
- Handle validation errors and return error responses
- Include all imports
- Show the corresponding MediatorK handler
```

**Example Usage:**
```
User: "Create a Spring REST endpoint POST /orders that creates an order using MediatorK"
Claude: [generates OrderController with mediator injection, request/response mapping, error handling]
```

---

## Prompt 8: Refactor to Vertical Slices

```
I have code organized in layers (data, domain, ui). Refactor it to use MediatorK vertical slice architecture:

[paste your current code]

Requirements:
- Each feature becomes a self-contained slice
- Request owns the request + handler + registrar + model
- No cross-feature dependencies
- Show the new directory structure
- Generate handlers with registrars
- Include all imports
```

**Example Usage:**
```
User: [paste LayeredOrderService + OrderRepository + OrderViewModel]
Claude: [restructures into orders/ slice with Request, Handler, Registrar]
```

---

## Prompt 9: Create a Full Feature Slice

```
Using MediatorK 0.8.1, create a complete feature slice for:

[describe the feature, e.g. "payment processing: accept payment, validate card, charge API, publish PaymentProcessedEvent"]

Deliverables:
- Request class (ProcessPaymentCommand)
- Handler class with all logic
- Validator class with rules
- Notification class (PaymentProcessedEvent)
- Notification handlers (email receipt, analytics)
- Registrar class
- Directory structure

Include all imports and show folder organization.
```

---

## Prompt 10: Generate Test Harness

```
Using MediatorK 0.8.1 test utilities, create a test harness for:

[describe the feature you want to test, e.g. "creating an order with validation and notifications"]

Requirements:
- Use MediatorFactory.create() with test registrars
- Set up fixtures and test data
- Test the happy path
- Test validation failures
- Verify notifications are published
- Include all imports and assertions
```

---

## Tips for AI Code Generation

When using these prompts with Claude or ChatGPT:

1. **Be Specific**: "Create an order" is vague. "Create an order with validation, send confirmation email, and log to analytics" is clear.

2. **Show Context**: Paste your models/interfaces if they exist. AI can follow your style.

3. **Ask for Variations**: "Now create the NotificationHandlers for that event"

4. **Request Testing Code**: "Also create unit tests for that handler"

5. **Ask for Structure**: "Show the directory structure for this feature"

---

## Version & Installation

**Current Version**: 0.8.1

```kotlin
dependencies {
    implementation("io.github.fajrbahr:mediatork:0.8.1")
}
```

---

## Next Steps

- Read [Vertical Slice Architecture](vertical-slice.md) for deep pattern explanation
- Check [Samples](sample.md) for runnable code  
- See [Full API Docs](api.md) for complete reference
