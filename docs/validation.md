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

→ [Kotlin JVM](jvm.md)
