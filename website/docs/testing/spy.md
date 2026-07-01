---
id: spy
title: MediatorSpy
sidebar_label: MediatorSpy
---

# MediatorSpy

`MediatorSpy` wraps any `Mediator` and records every `send` and `publish` call while still delegating to the real
handlers underneath. No mocking library needed.

Use it when you want to assert *what* was dispatched: which requests were sent, which notifications were published, and
how many times, while your actual handler logic still runs and returns real results.

---

## Setup

```kotlin
// Wrap any real mediator — FakeMediator, or your production one
val spy = MediatorSpy(
    FakeMediator {
        +CreateOrderHandler()
        +FetchUserHandler()
    }
)
```

Inject `spy` wherever your production code expects a `Mediator`. All `send` and `publish` calls go through it.

---

## Asserting requests were sent

```kotlin
@Test
fun `checkout sends CreateOrderCommand`() = runTest {
    val spy = MediatorSpy(FakeMediator { +CreateOrderHandler() })
    val checkout = CheckoutService(spy)

    checkout.placeOrder(cartId = "CART-1")

    // at least one was sent
    spy.assertSent<CreateOrderCommand>()

    // exact count
    spy.assertSentCount<CreateOrderCommand>(1)

    // inspect the actual value
    val cmd = spy.sentOf<CreateOrderCommand>().first()
    assertEquals("CART-1", cmd.cartId)
}
```

---

## Asserting nothing was sent

```kotlin
@Test
fun `checkout does not send command when cart is empty`() = runTest {
    val spy = MediatorSpy(FakeMediator { +CreateOrderHandler() })
    val checkout = CheckoutService(spy)

    checkout.placeOrder(cartId = "") // empty cart → no command

    spy.assertNotSent<CreateOrderCommand>()
}
```

---

## Asserting notifications were published

```kotlin
@Test
fun `order placement publishes OrderPlacedEvent`() = runTest {
    val fake = FakeMediator {
        +CreateOrderHandler()
        registerNotification(object : NotificationHandler<OrderPlacedEvent> {
            override suspend fun handle(notification: OrderPlacedEvent) = Unit
        })
    }
    val spy = MediatorSpy(fake)
    val checkout = CheckoutService(spy)

    checkout.placeOrder(cartId = "CART-1")

    spy.assertPublished<OrderPlacedEvent>()
    assertEquals("CART-1", spy.publishedOf<OrderPlacedEvent>().first().cartId)
}
```

---

## Full spy API

| Member                                 | Description                                          |
|----------------------------------------|------------------------------------------------------|
| `sentRequests`                         | All requests passed to `send`, in order              |
| `publishedNotifications`               | All notifications passed to `publish`, in order      |
| `sentOf<T>()`                          | Filtered list of sent requests of type `T`           |
| `publishedOf<T>()`                     | Filtered list of published notifications of type `T` |
| `assertSent<T>(message?)`              | Fails if no request of type `T` was sent             |
| `assertNotSent<T>(message?)`           | Fails if any request of type `T` was sent            |
| `assertSentCount<T>(n, message?)`      | Fails if sent count ≠ `n`                            |
| `assertPublished<T>(message?)`         | Fails if no notification of type `T` was published   |
| `assertNotPublished<T>(message?)`      | Fails if any notification of type `T` was published  |
| `assertPublishedCount<T>(n, message?)` | Fails if published count ≠ `n`                       |
| `reset()`                              | Clears all recorded sends and publishes              |

---

## Resetting between scenarios

```kotlin
@Test
fun `two independent scenarios in one test`() = runTest {
    val spy = MediatorSpy(FakeMediator { +CreateOrderHandler() })

    spy.send(CreateOrderCommand(id = "ORD-1"))
    spy.assertSentCount<CreateOrderCommand>(1)

    spy.reset()

    spy.send(CreateOrderCommand(id = "ORD-2"))
    spy.assertSentCount<CreateOrderCommand>(1) // count starts fresh after reset
}
```

---

Not sure whether you need a spy? See [Choosing the right helper](fake-mediator.md#choosing-the-right-helper).

---

## Next

→ [Testing Notifications](notification-testing.md)
