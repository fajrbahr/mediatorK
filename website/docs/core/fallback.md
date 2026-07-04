---
id: fallback
title: Fallback Chains
sidebar_label: Fallback Chains
---

# Fallback Chains

Fallback chains allow you to try multiple approaches for the same request or notification type. Each
candidate is tried in order; the first one that succeeds wins, and the rest are skipped. If every approach throws, the
last exception is re-thrown.

This is useful any time you want resilient dispatch within your handlers:

- Call a live API, fall back to a cache, fall back to a stub.
- Try a fast in-memory path, fall back to a database.
- Route to a feature-flagged implementation, fall back to the stable one.

---

## Requests

```kotlin
registry.handle<CreateOrderCommand, Order> { request ->
    try {
        api.create(request.cartId) // throws if the live API is down
    } catch (e: Exception) {
        try {
            cache.createFromCache(request.cartId)
        } catch (e2: Exception) {
            Order.stub()
        }
    }
}
```

Dispatch is unchanged; callers don't know a fallback chain exists:

```kotlin
val order: Order = mediator.send(CreateOrderCommand(cartId = "cart-42"))
```

---

## Notifications

```kotlin
registry.on<OrderShippedNotification> { notification ->
    try {
        push.send(notification.userId, "Your order shipped!") // throws if push service is down
    } catch (e: Exception) {
        email.send(notification.userId, "Your order shipped!")
    }
}
```

---

## Behavior at a glance

| Scenario                      | Result                                                 |
|-------------------------------|--------------------------------------------------------|
| First block succeeds          | Returns immediately, rest are skipped                  |
| First throws, second succeeds | Second result returned                                 |
| All blocks throw              | Last exception re-thrown                               |

---

## Next

→ [Kotlin JVM](../integration/jvm.md)
