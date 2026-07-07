---
id: validation
title: Validation
sidebar_label: Validation
---

# Validation

MediatorK ships a lightweight validation API in the `com.fajrbahr.mediatork.validator` package, with no annotation
processing, no reflection, no external dependencies.

---

## RequestValidator

Implement `RequestValidator<TRequest>` and return a `ValidationResult` to signal pass or fail:

```kotlin
interface RequestValidator<TRequest : Any> {
    fun validate(request: TRequest): ValidationResult
}
```

### Fail-fast: stop at first error

Use `rulesFailFast { }` to stop at the first error:

```kotlin
class CreateTodoValidator : RequestValidator<CreateTodoCommand> {
    override fun validate(request: CreateTodoCommand): ValidationResult = rulesFailFast {
        check(request.title.isNotBlank()) { "Title must not be blank" }
        check(request.title.length <= 200) { "Title must be 200 characters or fewer" }
        check(request.dueDate.isAfter(today())) { "Due date must be in the future" }
    }
}
```

Execution stops at the first failure. Only the first error is included in the result.

### Collect all errors: `rules { }`

Use `rules { }` when you want every failure reported in one pass (e.g. form validation):

```kotlin
class CreateInvoiceValidator : RequestValidator<CreateInvoiceCommand> {
    override fun validate(request: CreateInvoiceCommand): ValidationResult = rules {
        check(request.id.isNotBlank()) { "Invoice ID is required" }
        check(request.id.startsWith("INV-")) { "Invoice ID must start with INV-" }
        check(request.amount > 0) { "Amount must be positive" }
    }
}
```

All checks run regardless of earlier failures. `ValidationException.errors` contains the full list.

### Return directly

Return `ValidationResult.Invalid` when you have a single computed message:

```kotlin
class CreateInvoiceValidator : RequestValidator<CreateInvoiceCommand> {
    override fun validate(request: CreateInvoiceCommand): ValidationResult {
        if (request.id.isBlank()) return ValidationResult.Invalid("Invoice ID is required")
        return ValidationResult.Valid
    }
}
```

---

## Registering validators

Register validators via `HandlerRegistry.registerValidator<TRequest>(validator)`:

```kotlin
class AppRegistrar(
    private val repo: InvoiceRepository,
) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry register CreateInvoiceHandler(repo)
        registry.registerValidator(CreateInvoiceValidator())
    }
}
```

Or using the `+` shorthand inside a `scope { }` block:

```kotlin
registry.scope {
    +CreateInvoiceHandler(repo)
    +CreateInvoiceValidator()
}
```

---

## ValidationBehavior

MediatorK ships a ready-to-use `ValidationBehavior` that runs automatically when any validators are registered.
When you call `MediatorFactory.create`, it detects registered validators and injects `ValidationBehavior` at
`order = -50` automatically, with no manual setup needed.

If you need to customize the behavior order, you can construct and add it explicitly:

```kotlin
val mediator = MediatorFactory.create(
    registrars = listOf(AppRegistrar()),
    pipelineBehaviors = listOf(
        // ValidationBehavior is injected automatically from registered validators,
        // but you can override its order by passing it explicitly:
        ValidationBehavior(validators = registry.anyValidators(), order = -100),
    ),
)
```

`ValidationBehavior` runs before the handler for every request whose type has registered validators.
It throws `ValidationException` if any validator returns `ValidationResult.Invalid`.

:::tip
`ValidationBehavior` runs at `order = -50` by default so it executes before most behaviors.
:::

---

## Handling ValidationException

Catch `ValidationException` in your ViewModel or exception handler:

```kotlin
try {
    mediator.send(CreateTodoCommand(title, dueDate))
} catch (e: ValidationException) {
    errorMessage = e.errors.joinToString(", ") { it.toString() }
}
```

`ValidationException.errors` is a `List<*>`; cast to your error type if you used a custom type in `rules { }`.

---

## ValidationResult API

```kotlin
sealed class ValidationResult {
    data object Valid : ValidationResult()
    data class Invalid(val errors: List<*>) : ValidationResult()
}

// Throw directly if invalid
result.throwIfInvalid()
```

---

## Using Kotlin's `require` / `check`

You can use Kotlin's built-in `require` and `check` directly inside a handler instead of a `RequestValidator`. Both throw `IllegalArgumentException` on failure and integrate naturally with MediatorK's exception handling.

```kotlin
class CreateOrderHandler(private val orders: OrderRepository) : RequestHandler<CreateOrderCommand, OrderId> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: CreateOrderCommand,
    ): OrderId {
        require(request.items.isNotEmpty()) { "Order must contain at least one item" }
        require(request.totalAmount > 0) { "Order total must be positive" }
        val id = orders.save(request.toOrder())
        mediator.publish(OrderCreatedNotification(id))
        return id
    }
}
```

Use `require` for preconditions on input values, `check` for invariants on internal state. Once validation passes, use `mediator.publish()` to fan out notifications to any interested handlers, with no direct coupling between the handler and its subscribers. Prefer `RequestValidator` when you need structured `ValidationResult` objects (e.g. returning multiple errors to a UI).

---

## Next

→ [Requests & Handlers](requests.md)  
→ [Kotlin JVM](../integration/jvm.md)
