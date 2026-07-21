---
id: test-mediator
title: testMediator
sidebar_label: testMediator
---

# testMediator

There are no mocks and no stubs in `mediatork-test`. A handler is just a lambda that returns a value, so a
test double is just the real thing with a shorter handler body.

`testMediator { }` builds a **real** [`Mediator`](../core/dsl-builder.md) using the exact same DSL you use in
production — `handle`, `handleStream`, `notification`, `validate`, `behaviors`. The mediator it returns
([`RecordingMediator`](#recordingmediator)) additionally records every request and notification so you can assert on
what was dispatched.

```kotlin
import com.fajrbahr.mediatork.test.testMediator

val mediator = testMediator {
    handle<GetUserQuery, UserModel> { UserModel("User-${it.id}") }
}

val user = mediator.send(GetUserQuery("42"))   // runs the real pipeline
assertEquals("User-42", user.name)
assertEquals(1, mediator.sentOf<GetUserQuery>().size)
```

Best for **ViewModel tests** and **integration tests** where you control every response.

---

## Request handlers

Register a handler body that returns a value. Both the request and result type are given explicitly (Kotlin can't infer
the result type from a lambda literal):

```kotlin
val mediator = testMediator {
    handle<GetUserQuery, UserModel> { query -> UserModel("User-${query.id}") }
    handle<CreateOrderCommand, OrderResult> { cmd -> OrderResult(cmd.itemId) }
}
```

Throw from the body to simulate a failure — no special "throws" API:

```kotlin
val mediator = testMediator {
    handle<CreateOrderCommand, OrderResult> { error("out of stock") }
}
assertFailsWith<IllegalStateException> { mediator.send(CreateOrderCommand("A1")) }
```

`send` throws `MissingHandlerException` if no handler is registered for the request type — exactly like production.

### With a validator

Register a real `validate<T>` and invalid requests throw `ValidationException` before the handler runs, exactly like the
real pipeline:

```kotlin
val mediator = testMediator {
    handle<CreateOrderCommand, OrderResult> { OrderResult(it.itemId) }
    validate<CreateOrderCommand> { cmd ->
        rules { check(cmd.itemId.isNotBlank()) { "itemId required" } }
    }
}
```

You can reuse a validator you already ship (e.g. `validate<CreateOrderCommand>(createOrderValidator)`) so the test
exercises the same rules as the app.

---

## Notification handlers

```kotlin
val received = mutableListOf<OrderPlacedEvent>()
val mediator = testMediator {
    notification<OrderPlacedEvent> { received += it }
}

mediator.publish(OrderPlacedEvent("ORD-1"))

assertEquals(1, received.size)
assertEquals(1, mediator.publishedOf<OrderPlacedEvent>().size)
```

Publishing a notification with no registered listener throws `MissingNotificationHandlerException` — the real pipeline
behaviour. Register a listener (even an empty one) for events your code fires, or set `onMissingNotification` in the
block.

---

## Stream handlers

```kotlin
val mediator = testMediator {
    handleStream<StreamItemsQuery, String> { query -> flow { emit("${query.prefix}-1") } }
}
val items = mediator.stream(StreamItemsQuery("x")).toList()
```

---

## Pipeline behaviors

Add real behaviors with the production `behaviors(...)` DSL — including the
[built-in behaviors](../core/built-in-behaviors.mdx):

```kotlin
val mediator = testMediator {
    handle<GetUserQuery, UserModel> { UserModel(it.id) }
    behaviors(logging(), timing())
}
```

---

## RecordingMediator

`testMediator { }` returns a `RecordingMediator`: a thin wrapper that delegates every call to a real mediator and
records what passed through. Use it directly to record around a mediator you assembled from production modules:

```kotlin
import com.fajrbahr.mediatork.test.RecordingMediator

val mediator = RecordingMediator(
    mediatorK {
        courseModule(store, deptStore, studentStore)
        studentModule(studentStore, courseStore)
    }
)

val service = CheckoutService(mediator)
service.placeOrder(cartId = "CART-1")

assertEquals("CART-1", mediator.sentOf<CreateOrderCommand>().first().cartId)
```

### Recording API

| Member                   | Description                                          |
|--------------------------|------------------------------------------------------|
| `sent`                   | Every request passed to `send`/`stream`, in order    |
| `published`              | Every notification passed to `publish`, in order     |
| `sentOf<T>()`            | Recorded requests of type `T`, in order              |
| `publishedOf<T>()`       | Recorded notifications of type `T`, in order         |

Assertions are just standard list checks (`sentOf<T>().size`, `sent.isEmpty()`, …) — no bespoke `assertSent` helpers to
learn.

---

## Choosing the right double

| Situation                                          | Use                                              |
|----------------------------------------------------|--------------------------------------------------|
| Test controls what `send`/`publish` do             | `testMediator { }`                               |
| Test also asserts *which* requests were dispatched | `testMediator { }` then `sentOf<T>()`            |
| Test records around already-assembled modules      | `RecordingMediator(mediatorK { … })`             |
| Constructor needs a `Mediator` but `send` is never called | `testMediator { }` (an empty real mediator) |

There's no lenient no-op double on purpose: an empty `testMediator { }` is a real mediator, so an unexpected `send`
fails loudly with `MissingHandlerException` instead of silently returning nothing.

Need something more specific? `Mediator` is a plain interface — implement, wrap, or decorate it directly.

---

## Next

→ [Testing ViewModels](viewmodel-testing.md)
