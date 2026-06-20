---
id: validation
title: Validation
sidebar_label: Validation
---

# Validation

MediatorK ships a lightweight validation API in the `com.fajrbahr.mediatork.validator` package — no annotation
processing, no reflection, no external dependencies.

---

## RequestValidator

Implement `RequestValidator<TRequest>` and return a `ValidationResult` to signal pass or fail:

```kotlin
interface RequestValidator<TRequest : Any> {
    fun validate(request: TRequest): ValidationResult
}
```

### Fail-fast — stop at first error

Use `rulesFailFast { }` or Kotlin's own `require`/`check` (thrown as `IllegalArgumentException`):

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

### Collect all errors — `rules { }`

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
`order = -50` automatically — no manual setup needed.

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

`ValidationException.errors` is a `List<*>` — cast to your error type if you used a custom type in `rules { }`.

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

## Next

→ [Requests & Handlers](requests.md)  
→ [Kotlin JVM](../integration/jvm.md)
