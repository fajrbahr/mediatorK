---
id: validation
title: Validation
sidebar_label: Validation
---

# Validation

MediatorK ships a lightweight validation API in the `com.fajrbahr.mediatork.validator` package — no annotation
processing, no reflection, no external dependencies.

---

## Typed field identifiers

Use an enum or sealed class so the compiler catches typos and `when` expressions are exhaustive:

```kotlin
enum class CreateTodoFields : FieldValidator {
    TITLE, DUE_DATE
}
```

`DefaultField` is used when the error isn't tied to a specific field.

---

## ValidationResult

```kotlin
// success
val ok = ValidationResult.Success

// single field-agnostic error
val err = ValidationResult.error("Something went wrong")

// error tied to a specific field
val fieldErr = ValidationResult.error(CreateTodoFields.TITLE, "Title must not be blank")

// multiple errors
val multi = ValidationResult.failure(
    ValidationError(CreateTodoFields.TITLE, "Required"),
    ValidationError(CreateTodoFields.DUE_DATE, "Must be in the future"),
)
```

`ValidationResult.isValid` is `true` when `errors` is empty.

---

## RequestValidator

### Basic style

Early-return as soon as the first rule fails:

```kotlin
class CreateTodoValidator : RequestValidator<CreateTodoCommand> {
    override val requestClass = CreateTodoCommand::class

    override fun validate(request: CreateTodoCommand): ValidationResult {
        if (request.title.isBlank())
            return ValidationResult.error(CreateTodoFields.TITLE, "Title must not be blank")
        return ValidationResult.Success
    }
}
```

### rules { } style — collect all errors

`rules { }` evaluates every rule and collects all failures in one pass. Use `ruleFor` to scope checks to a specific
field:

```kotlin
class CreateTodoValidator : RequestValidator<CreateTodoCommand> {
    override val requestClass = CreateTodoCommand::class

    override fun validate(request: CreateTodoCommand): ValidationResult = rules {
        ruleFor(CreateTodoFields.TITLE, request.title) { value ->
            check(value.isNotBlank()) { "Title must not be blank" }
            check(value.length <= 200) { "Title must be 200 characters or fewer" }
        }
        ruleFor(CreateTodoFields.DUE_DATE, request.dueDate) { value ->
            check(value.isAfter(today())) { "Due date must be in the future" }
        }
    }
}
```

### rulesFailFast { } — stop at first error

Use `rulesFailFast { }` when later rules depend on earlier ones being valid (e.g. parse before validate):

```kotlin
override fun validate(request: CreateTodoCommand): ValidationResult = rulesFailFast {
    check(request.title.isNotBlank()) { "Title must not be blank" }
    check(request.title.length <= 200) { "Title must be 200 characters or fewer" }
}
```

---

## ValidationBehavior

MediatorK ships a ready-to-use `ValidationBehavior` — just pass your validators and register it:

```kotlin
val mediator = MediatorFactory.create(
    registrars = listOf(AppRegistrar()),
    pipelineBehaviors = listOf(
        ValidationBehavior(
            validators = listOf(CreateTodoValidator(), UpdateTodoValidator()),
        ),
    ),
)
```

:::tip
`ValidationBehavior` runs at `order = -50` by default so it executes before most behaviors. You can override the order,
replace it entirely with your own `PipelineBehavior`, or skip it and call validators directly from your handlers —
whichever fits your setup.
:::

When validation fails, `ValidationBehavior` throws `ValidationException`. Catch it to map errors to UI state.

---

## Handling errors by field type in a ViewModel

Because `FieldValidator` is a typed interface implemented by your enum, use `when` to dispatch each error to the right
UI state — exhaustive, no string matching:

```kotlin
class CreateTodoViewModel(private val mediator: Mediator) : ViewModel() {

    val titleError   = MutableStateFlow<String?>(null)
    val dueDateError = MutableStateFlow<String?>(null)
    val generalError = MutableStateFlow<String?>(null)

    fun submit(title: String, dueDate: LocalDate) {
        viewModelScope.launch {
            try {
                mediator.send(CreateTodoCommand(title, dueDate))
            } catch (e: ValidationException) {
                e.errors.forEach { error ->
                    when (error.field) {
                        CreateTodoFields.TITLE    -> titleError.value   = error.message
                        CreateTodoFields.DUE_DATE -> dueDateError.value = error.message
                        else                      -> generalError.value  = error.message
                    }
                }
            }
        }
    }
}
```

Each `StateFlow` maps directly to one field in the UI — no `field == "TITLE"` string comparison.

---

## Next

→ [Kotlin JVM](../integration/jvm.md)
