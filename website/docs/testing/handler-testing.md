---
id: handler-testing
title: Testing Handlers
sidebar_label: Testing Handlers
---

# Testing Handlers

## Handlers are glue, not logic

A handler's job is to orchestrate: call a repository, map a result, return a value. It is a glue class, not a
business-logic class. The distinction matters because it determines how you test it.

**Integration test the handler.** Wire it against a real (or in-memory) database, a real repository, or a real service.
The handler test verifies that the pieces connect correctly.

**Unit test the business logic.** Keep business rules in pure value classes or domain objects that have no dependencies.
Those are trivially testable with no setup at all.

The goal for unit tests: avoid mocks wherever possible. If a test needs a mock, that is usually a sign that logic which
belongs in a pure class is still sitting inside the handler.

---

## Before: logic inside the handler

```kotlin
fun HandlerRegistry.validateCard(cardRepository: CardRepository) = handle<ValidateCardCommand> { request ->
    // date parsing and validation logic living inside the handler
    val onlyDigits = request.expiryDate.filter { it.isDigit() }
    if (onlyDigits.length != 4) return@handle ValidationResult.Invalid("Bad date format")

    val month = onlyDigits.substring(0, 2).toInt()
    val year  = onlyDigits.substring(2, 4).toInt()
    if (month < 1 || month > 12) return@handle ValidationResult.Invalid("Invalid month")

    val card = cardRepository.find(request.cardId)
        ?: return@handle ValidationResult.Invalid("Card not found")

    if (card.expiryMonth == month && card.expiryYear == year)
        ValidationResult.Valid
    else
        ValidationResult.Invalid("Expiry mismatch")
}
```

To unit test the date validation branch you need to stub `CardRepository`. The logic and the I/O are tangled together.

---

## After: logic extracted to a pure value class

Move the rules out of the handler into an `@Immutable` value class with no dependencies:

```kotlin
@Immutable
data class ExpirationDate(val value: String) {

    companion object {
        fun create(raw: String): ExpirationDate = ExpirationDate(raw.filter { it.isDigit() })
        fun empty() = ExpirationDate("")
    }

    val isValid: Boolean
        get() = value.length == 4 && month in 1..12

    val formatted: String
        get() = value.chunked(2).joinToString("/")

    val month: Int get() = value.substring(0, 2).toInt()
    val year: Int  get() = value.substring(2, 4).toInt()
}
```

The handler becomes glue; it delegates validation to `ExpirationDate` and touches the repository only when the input is
already known-good:

```kotlin
fun HandlerRegistry.validateCard(cardRepository: CardRepository) = handle<ValidateCardCommand> { request ->
    val expiry = ExpirationDate.create(request.expiryDate)
    if (!expiry.isValid) return@handle ValidationResult.Invalid("Bad expiry date")

    val card = cardRepository.find(request.cardId)
        ?: return@handle ValidationResult.Invalid("Card not found")

    if (card.expiryMonth == expiry.month && card.expiryYear == expiry.year)
        ValidationResult.Valid
    else
        ValidationResult.Invalid("Expiry mismatch")
}
```

---

## Unit test the pure class: no mocks, no setup

```kotlin
class ExpirationDateTest {

    @Test
    fun `valid date passes`() {
        assertTrue(ExpirationDate.create("1228").isValid)
    }

    @Test
    fun `invalid month fails`() {
        assertFalse(ExpirationDate.create("1328").isValid)
    }

    @Test
    fun `non-digits are stripped`() {
        assertEquals("1228", ExpirationDate.create("12/28").value)
    }

    @Test
    fun `formats as MM slash YY`() {
        assertEquals("12/28", ExpirationDate.create("1228").formatted)
    }
}
```

No `FakeMediator`, no stubbed repository, no coroutine test scope. The rule is: if a test requires a mock, the logic
probably belongs somewhere else.

---

## Integration test the handler

The handler test verifies the wiring (repository call, result mapping), not the business rules:

```kotlin
class ValidateCardHandlerTest {

    private val repository = InMemoryCardRepository()
    private val mediator = FakeMediator {
        validateCard(repository)
    }

    @Test
    fun `returns valid for matching expiry`() = runTest {
        repository.save(Card(id = "c1", expiryMonth = 12, expiryYear = 28))

        val result = mediator.send(ValidateCardCommand(cardId = "c1", expiryDate = "1228"))

        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun `returns invalid when card not found`() = runTest {
        val result = mediator.send(ValidateCardCommand(cardId = "missing", expiryDate = "1228"))

        assertEquals(ValidationResult.Invalid("Card not found"), result)
    }
}
```

The test is short because there is nothing to fake; the date logic is already proven by `ExpirationDateTest`, and the
handler test only checks the paths that require the repository.

---

## Custom mediator

Remember, you can always extend `Mediator` however you need. `FakeMediator` and `DummyMediator` are conveniences, not
constraints. If your test scenario calls for something more specific, implement the interface directly:

```kotlin
class CapturingMediator : Mediator {
    val sent = mutableListOf<Any>()

    @Suppress("UNCHECKED_CAST")
    override suspend fun <TRequest : Request<TResult>, TResult> send(request: TRequest): TResult {
        sent += request
        return Unit as TResult
    }

    override fun <TRequest : StreamRequest<T>, T> stream(request: TRequest): Flow<T> = emptyFlow()

    override suspend fun <T : Notification> publish(notification: T) = Unit
    override suspend fun <T : Notification> publish(notification: T, publisher: NotificationPublishStrategy) = Unit
}
```

The `Mediator` interface is yours to implement, wrap, decorate, or proxy in any way the test requires.

---

## Next

→ [Testing ViewModels](viewmodel-testing.md)
