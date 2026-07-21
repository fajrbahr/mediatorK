---
id: notification-testing
title: Testing Notifications
sidebar_label: Testing Notifications
---

# Testing Notifications

## Capturing published notifications

Register a `notification<T>` listener in the `testMediator { }` block and let it append to a list. There is nothing to
mock — the listener is the real thing.

```kotlin
@Test
fun `order placed event is published`() = runTest {
    val events = mutableListOf<OrderPlacedEvent>()
    val mediator = testMediator {
        handle<CreateOrderCommand, OrderResult> { OrderResult(orderId = it.id) }
        notification<OrderPlacedEvent> { events += it }
    }

    // your code under test publishes OrderPlacedEvent as part of handling the command
    mediator.send(CreateOrderCommand(id = "ORD-1", amount = 99.0))

    assertEquals(1, events.size)
    assertEquals("ORD-1", events.first().orderId)
}
```

If the code under test publishes directly through the mediator, you can also assert on the recording instead of a
listener:

```kotlin
mediator.publish(OrderPlacedEvent("ORD-1"))
assertEquals(1, mediator.publishedOf<OrderPlacedEvent>().size)
```

---

## Custom logic in a listener

The listener body is a plain lambda, so put whatever assertions or side effects you need inside it:

```kotlin
@Test
fun `analytics is tracked on order placed`() = runTest {
    val tracked = mutableListOf<String>()
    val mediator = testMediator {
        handle<CreateOrderCommand, OrderResult> { OrderResult(orderId = it.id) }
        notification<OrderPlacedEvent> { tracked += "tracked:${it.orderId}" }
    }

    mediator.send(CreateOrderCommand(id = "ORD-1", amount = 99.0))

    assertEquals("tracked:ORD-1", tracked.first())
}
```

---

## Multiple listeners for the same notification

`notification<T>` can be registered several times; every listener fires, in `order`:

```kotlin
@Test
fun `all listeners receive the event`() = runTest {
    val emails = mutableListOf<OrderPlacedEvent>()
    val sms    = mutableListOf<OrderPlacedEvent>()
    val mediator = testMediator {
        notification<OrderPlacedEvent> { emails += it }
        notification<OrderPlacedEvent> { sms += it }
    }

    mediator.publish(OrderPlacedEvent("ORD-1"))

    assertEquals(1, emails.size)
    assertEquals(1, sms.size)
}
```

---

## Next

→ [Testing Handlers](handler-testing.md)
