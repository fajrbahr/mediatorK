---
id: validation
title: Validation
sidebar_label: Validation
---

# Validation

MediatorK ships a lightweight validation API in the `com.fajrbahr.mediatork.validator` package, with no annotation
processing, no reflection, no external dependencies.

---

## Inline Validation

The simplest way to validate a request is to override `validate()` directly on the request class.
No separate class, no registration — the validation lives right next to the data it validates:

```kotlin
data class CreateTodoCommand(
    val title: String,
    val dueDate: LocalDate,
) : Request<TodoId> {
    override fun validate() = rules<String> {
        check(title.isNotBlank()) { "Title must not be blank" }
        check(title.length <= 200) { "Title must be 200 characters or fewer" }
        check(dueDate.isAfter(today())) { "Due date must be in the future" }
    }
}
```

All checks run regardless of earlier failures. `ValidationException.errors` contains the full list.

### Fail-fast: stop at first error

Use `rulesFailFast { }` to stop at the first failure:

```kotlin
data class GetOrderQuery(
    val orderId: String,
    val customerId: String,
) : Request<OrderDetails> {
    override fun validate() = rulesFailFast<String> {
        check(orderId.isNotBlank()) { "Order ID is required" }
        check(orderId.startsWith("ORD-")) { "Order ID must start with ORD-" }
        check(customerId.isNotBlank()) { "Customer ID is required" }
    }
}
```

Execution stops at the first failure. Only the first error is included in the result.

### Return directly

Return `ValidationResult.Invalid` when you have a single computed message:

```kotlin
data class CreateInvoiceCommand(
    val id: String,
    val amount: Double,
) : Request<InvoiceId> {
    override fun validate(): ValidationResult {
        if (id.isBlank()) return ValidationResult.Invalid("Invoice ID is required")
        return ValidationResult.Valid
    }
}
```

---

## RequestValidator (External)

When a validator needs injected dependencies (a repository, a service), use the `validate` DSL and register it with the `HandlerRegistry`:

```kotlin
registry.validate<CreateUserCommand> { request ->
    rules<String> {
        check(!userRepo.existsByEmail(request.email)) { "Email already taken" }
    }
}
```

Register it alongside the handler in your registrar:

```kotlin
class ValidationRegistrar(private val userRepo: UserRepository) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.validate<CreateUserCommand> { request ->
            rules<String> {
                check(!userRepo.existsByEmail(request.email)) { "Email already taken" }
            }
        }
        
        registry.handle<CreateUserCommand, User> { request ->
            // implementation
            TODO()
        }
    }
}
```

Both inline `validate()` and registered validators can coexist on the same request type.
Inline validation runs first, then any registered validators.

---

## ValidationBehavior

MediatorK ships a ready-to-use `ValidationBehavior`, and `MediatorFactory.create` adds it to the pipeline
automatically at `order = -50`; no manual setup needed.

`ValidationBehavior` runs before the handler for every request. It first calls the request's own
`validate()` method, then runs any registered `RequestValidator`s for that request type.
It throws `ValidationException` if any validation returns `ValidationResult.Invalid`.

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

You can use Kotlin's built-in `require` and `check` directly inside a handler instead of overriding `validate()`.
`require` throws `IllegalArgumentException` and `check` throws `IllegalStateException`; both integrate naturally with
MediatorK's exception handling.

```kotlin
registry.handle<CreateOrderCommand, OrderId> { request ->
    require(request.items.isNotEmpty()) { "Order must contain at least one item" }
    require(request.totalAmount > 0) { "Order total must be positive" }
    val id = orders.save(request.toOrder())
    mediator.publish(OrderCreatedNotification(id))
    id
}
```

Use `require` for preconditions on input values, `check` for invariants on internal state. Prefer inline `validate()` on
the request when you need structured `ValidationResult` objects (e.g. returning multiple errors to a UI).

---

## Next

→ [MediatorFactory](factory.md)
