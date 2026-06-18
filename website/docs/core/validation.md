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

## Validation Scopes

Real-world validation happens at three distinct points in the lifecycle. MediatorK formalises this with `ValidationScope`:

| Scope         | When it runs                          | Automatic?         | What to check                          |
|---------------|---------------------------------------|--------------------|----------------------------------------|
| `REQUEST`     | Before the handler, in the pipeline   | Yes (via `ValidationBehavior`) | Field format and type — answerable from the request alone |
| `DOMAIN`      | Inside the handler, after loading state | No — call explicitly | Business rules that need the loaded aggregate |
| `PERSISTENCE` | Inside the handler, before writing    | No — call explicitly | DB constraints (uniqueness, FK checks) |

Declare the scope on your validator:

```kotlin
// REQUEST — runs automatically in the pipeline (never touches the DB)
class CreateInvoiceRequestValidator : RequestValidator<CreateInvoiceCommand> {
    override val requestClass = CreateInvoiceCommand::class
    override val scope = ValidationScope.REQUEST

    override fun validate(request: CreateInvoiceCommand): ValidationResult = rules {
        ruleFor(CreateInvoiceField.Id, request.id) {
            check(it.isNotBlank()) { "Invoice ID is required" }
            check(it.startsWith("INV-")) { "Invoice ID must start with INV-" }
        }
        ruleFor(CreateInvoiceField.Amount, request.amount) {
            check(it > 0) { "Amount must be positive" }
        }
    }
}

// DOMAIN — called by the handler after the aggregate is loaded
class CreateInvoiceDomainValidator(private val repo: InvoiceRepository)
    : RequestValidator<CreateInvoiceCommand> {
    override val requestClass = CreateInvoiceCommand::class
    override val scope = ValidationScope.DOMAIN

    override fun validate(request: CreateInvoiceCommand): ValidationResult =
        if (repo.findById(request.id) != null)
            ValidationResult.error(CreateInvoiceField.Id, "Invoice ${request.id} already exists")
        else ValidationResult.Success
}

// PERSISTENCE — called just before the write, ideally inside the transaction
class CreateInvoicePersistenceValidator(private val repo: InvoiceRepository)
    : RequestValidator<CreateInvoiceCommand> {
    override val requestClass = CreateInvoiceCommand::class
    override val scope = ValidationScope.PERSISTENCE

    override fun validate(request: CreateInvoiceCommand): ValidationResult =
        if (repo.findById(request.id) != null)
            ValidationResult.error(CreateInvoiceField.Id, "Duplicate invoice ID — database constraint violated")
        else ValidationResult.Success
}
```

The handler calls DOMAIN and PERSISTENCE validators explicitly at the right moment:

```kotlin
class CreateInvoiceHandler(
    private val repo: InvoiceRepository,
    private val domainValidator: CreateInvoiceDomainValidator,
    private val persistenceValidator: CreateInvoicePersistenceValidator,
) : RequestHandler<CreateInvoiceCommand, Unit> {

    override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: CreateInvoiceCommand) {
        // DOMAIN — needs the repo, runs after handler entry
        val domainResult = domainValidator.validate(request)
        if (!domainResult.isValid) throw ValidationException(domainResult.errors)

        val invoice = Invoice(id = request.id, amount = request.amount)

        // PERSISTENCE — runs just before the write (inside a transaction)
        val persistenceResult = persistenceValidator.validate(request)
        if (!persistenceResult.isValid) throw ValidationException(persistenceResult.errors)

        repo.save(invoice)
    }
}
```

`ValidationBehavior` in the pipeline will only run validators whose `scope == ValidationScope.REQUEST` — DOMAIN and PERSISTENCE validators are ignored by the pipeline even if you pass them in.

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

→ [Requests & Handlers](requests.md) — streaming requests, fallback chains  
→ [Kotlin JVM](../integration/jvm.md)
