---
id: validation
title: Validation
sidebar_label: Validation
---

# Validation

MediatorK ships a lightweight validation API in the `com.fajrbahr.mediatork.validator` package — no annotation processing, no reflection, no external dependencies.

---

## ValidationResult

```kotlin
// success
val ok = ValidationResult.Success

// single error (no specific field)
val err = ValidationResult.error("Title must not be blank")

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

## Typed field identifiers

Use a sealed class or enum so the compiler catches typos:

```kotlin
enum class CreateTodoFields : FieldV {
    TITLE, DUE_DATE
}
```

`DefaultField` is used when the error isn't tied to a specific field.

---

## RequestValidator

### Basic style

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

### ruleFor / check style

Build a small DSL on top of the existing API so each rule reads as one line and **all errors accumulate** before returning:

```kotlin
// ValidationBuilder.kt — put this anywhere in your project
fun <T> T.validate(block: ValidationBuilder<T>.() -> Unit): ValidationResult =
    ValidationBuilder(this).apply(block).build()

class ValidationBuilder<T>(private val request: T) {
    private val errors = mutableListOf<ValidationError>()

    fun ruleFor(field: FieldV, passes: Boolean) = RuleContext(field, passes, errors)

    fun build(): ValidationResult =
        if (errors.isEmpty()) ValidationResult.Success
        else ValidationResult.failure(*errors.toTypedArray())
}

class RuleContext(
    private val field: FieldV,
    private val passes: Boolean,
    private val errors: MutableList<ValidationError>,
) {
    infix fun check(message: String) {
        if (!passes) errors += ValidationError(field, message)
    }
}
```

Now the validator becomes a flat list of rules — every rule is evaluated and all failures are collected at once:

```kotlin
class CreateTodoValidator : RequestValidator<CreateTodoCommand> {
    override val requestClass = CreateTodoCommand::class

    override fun validate(request: CreateTodoCommand): ValidationResult =
        request.validate {
            ruleFor(CreateTodoFields.TITLE,    request.title.isNotBlank())       check "Title must not be blank"
            ruleFor(CreateTodoFields.DUE_DATE, request.dueDate.isAfter(today())) check "Due date must be in the future"
        }
}
```

---

## Handling errors by field type in a ViewModel

Because `FieldV` is a typed interface implemented by your enum, use `when` to dispatch each error to the right UI state — no string comparison needed, and the compiler warns you if you miss a case:

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

Each `StateFlow` maps directly to one field in the UI — no parsing, no `field == "TITLE"` string matching.

---

## Wiring validation into the pipeline

Validators are invoked by a `PipelineBehavior` you write — this keeps the decision of how to handle failures in your code:

```kotlin
class ValidationBehavior(
    private val validators: List<RequestValidator<*>>,
) : PipelineBehavior {
    override val order = -50 // run early

    @Suppress("UNCHECKED_CAST")
    override suspend fun <TReq : Request<TRes>, TRes> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TReq, TRes>,
        request: TReq,
    ): TRes {
        val validator = validators.firstOrNull {
            it.requestClass.isInstance(request)
        } as? RequestValidator<TReq>

        val result = validator?.validate(request)
        if (result != null && !result.isValid)
            throw ValidationException(result.errors)

        return next(request)
    }
}
```

Register it with `MediatorFactory`:

```kotlin
val mediator = MediatorFactory.create(
    registrars = listOf(AppRegistrar()),
    pipelineBehaviors = listOf(
        ValidationBehavior(listOf(CreateTodoValidator())),
    ),
)
```

---

## Next

→ [Kotlin JVM](../integration/jvm.md)
