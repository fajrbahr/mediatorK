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

Implement `RequestValidator<TRequest>` and **throw** to signal failure. The library imposes no return type.

### Fail-fast — stop at first error

Use Kotlin's `require` or `check`:

```kotlin
class CreateTodoValidator : RequestValidator<CreateTodoCommand> {
    override val requestClass = CreateTodoCommand::class

    override fun validate(request: CreateTodoCommand) {
        require(request.title.isNotBlank()) { "Title must not be blank" }
        require(request.title.length <= 200) { "Title must be 200 characters or fewer" }
        require(request.dueDate.isAfter(today())) { "Due date must be in the future" }
    }
}
```

### Collect all errors — `rules { }`

Use `rules { }` when you want every failure reported in one pass (e.g. form validation):

```kotlin
class CreateInvoiceRequestValidator : RequestValidator<CreateInvoiceCommand> {
    override val requestClass = CreateInvoiceCommand::class

    override fun validate(request: CreateInvoiceCommand) = rules {
        check(request.id.isNotBlank()) { "Invoice ID is required" }
        check(request.id.startsWith("INV-")) { "Invoice ID must start with INV-" }
        check(request.amount > 0) { "Amount must be positive" }
    }
}
```

All checks run regardless of earlier failures. `ValidationException.errors` contains the full list.

### Throw directly

Throw `ValidationException` when you have a single computed message:

```kotlin
if (repo.findById(request.id) != null)
    throw ValidationException("Invoice ${request.id} already exists")
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

    override fun validate(request: CreateInvoiceCommand) {
        require(request.id.isNotBlank()) { "Invoice ID is required" }
        require(request.id.startsWith("INV-")) { "Invoice ID must start with INV-" }
        require(request.amount > 0) { "Amount must be positive" }
    }
}

// DOMAIN — called by the handler after the aggregate is loaded
class CreateInvoiceDomainValidator(private val repo: InvoiceRepository)
    : RequestValidator<CreateInvoiceCommand> {
    override val requestClass = CreateInvoiceCommand::class
    override val scope = ValidationScope.DOMAIN

    override fun validate(request: CreateInvoiceCommand) {
        if (repo.findById(request.id) != null)
            throw ValidationException("Invoice ${request.id} already exists")
    }
}

// PERSISTENCE — called just before the write, ideally inside the transaction
class CreateInvoicePersistenceValidator(private val repo: InvoiceRepository)
    : RequestValidator<CreateInvoiceCommand> {
    override val requestClass = CreateInvoiceCommand::class
    override val scope = ValidationScope.PERSISTENCE

    override fun validate(request: CreateInvoiceCommand) {
        if (repo.findById(request.id) != null)
            throw ValidationException("Duplicate invoice ID — database constraint violated")
    }
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
        domainValidator.validate(request)        // throws if duplicate

        val invoice = Invoice(id = request.id, amount = request.amount)

        persistenceValidator.validate(request)   // throws if DB constraint violated

        repo.save(invoice)
    }
}
```

`ValidationBehavior` in the pipeline will only run validators whose `scope == ValidationScope.REQUEST`.

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

`ValidationBehavior` wraps `IllegalArgumentException` (from `require`) and `IllegalStateException` (from `check`)
into `ValidationException`. A `ValidationException` thrown directly by a validator is re-thrown unchanged.

:::tip
`ValidationBehavior` runs at `order = -50` by default so it executes before most behaviors. You can override the
order or replace it entirely with your own `PipelineBehavior`.
:::

Catch `ValidationException` in your ViewModel or exception handler:

```kotlin
try {
    mediator.send(CreateTodoCommand(title, dueDate))
} catch (e: ValidationException) {
    errorMessage = e.message
}
```

---

## Next

→ [Requests & Handlers](requests.md) — streaming requests, fallback chains  
→ [Kotlin JVM](../integration/jvm.md)
