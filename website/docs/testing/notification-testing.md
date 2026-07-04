---
id: notification-testing
title: Testing Notifications
sidebar_label: Testing Notifications
---

# Testing Notifications

## captureNotifications

`captureNotifications<T>()` is the quickest way to assert that a notification was published. Call it on a
`FakeMediator`; it registers the handler and returns a live list that fills itself as notifications arrive.

```kotlin
@Test
fun `order placed event is published`() = runTest {
    val mediator = FakeMediator {
        handle<CreateOrderCommand> { createOrder() }
    }
    val events = mediator.captureNotifications<OrderPlacedEvent>()

    mediator.send(CreateOrderCommand(id = "ORD-1", amount = 99.0))

    assertEquals(1, events.size)
    assertEquals("ORD-1", events.first().orderId)
}
```

---

## on DSL

When you need full control over what happens inside the handler (side effects, conditional logic, custom assertions),
use the `on` DSL directly:

```kotlin
@Test
fun `analytics is tracked on order placed`() = runTest {
    val mediator = FakeMediator {
        handle<CreateOrderCommand> { createOrder() }
    }
    val tracked = mutableListOf<String>()
    mediator.registry.on<OrderPlacedEvent> { event ->
        tracked += "tracked:${event.orderId}"
    }

    mediator.send(CreateOrderCommand(id = "ORD-1", amount = 99.0))

    assertEquals("tracked:ORD-1", tracked.first())
}
```

---

## Multiple handlers for the same notification

`on` appends; you can register several handlers for the same type and all of them fire:

```kotlin
@Test
fun `all listeners receive the event`() = runTest {
    val mediator = FakeMediator {
        handle<CreateOrderCommand> { createOrder() }
    }
    val emails = mediator.captureNotifications<OrderPlacedEvent>()
    val sms    = mediator.captureNotifications<OrderPlacedEvent>()

    mediator.send(CreateOrderCommand(id = "ORD-1", amount = 99.0))

    assertEquals(1, emails.size)
    assertEquals(1, sms.size)
}
```

---

## Next

→ [Testing Handlers](handler-testing.md)
