---
id: fallback
title: Fallback Chains
sidebar_label: Fallback Chains
---

# Fallback Chains

The `otherwise` infix operator lets you chain multiple handlers for the same request or notification type. Each
candidate is tried in order — the first one that succeeds wins, and the rest are skipped. If every handler throws, the
last exception is re-thrown.

```
PrimaryHandler()  →  (throws?)  →  SecondaryHandler()  →  (throws?)  →  TertiaryHandler()
                                                                          ↓ re-throws if this also throws
```

This is useful any time you want resilient dispatch without wrapping your handlers in try/catch:

- Call a live API, fall back to a cache, fall back to a stub.
- Try a fast in-memory path, fall back to a database.
- Route to a feature-flagged implementation, fall back to the stable one.

---

## Requests

```kotlin
class CreateOrderHandler(private val api: OrderApi) : RequestHandler<CreateOrderCommand, Order> {
    override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: CreateOrderCommand): Order =
        api.create(request.cartId) // throws if the live API is down
}

class CreateOrderFallbackHandler(private val cache: OrderCache) : RequestHandler<CreateOrderCommand, Order> {
    override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: CreateOrderCommand): Order =
        cache.createFromCache(request.cartId)
}

class CreateOrderStubHandler : RequestHandler<CreateOrderCommand, Order> {
    override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: CreateOrderCommand): Order =
        Order.stub()
}
```

Register the chain with `otherwise`:

```kotlin
class OrderRegistrar(
    private val api: OrderApi,
    private val cache: OrderCache,
) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry register (
            CreateOrderHandler(api)
                otherwise CreateOrderFallbackHandler(cache)
                otherwise CreateOrderStubHandler()
        )
    }
}
```

Dispatch is unchanged — callers don't know a fallback chain exists:

```kotlin
val order: Order = mediator.send(CreateOrderCommand(cartId = "cart-42"))
```

---

## Notifications

```kotlin
class PushNotificationHandler(private val push: PushService) : NotificationHandler<OrderShippedNotification> {
    override suspend fun handle(notification: OrderShippedNotification) =
        push.send(notification.userId, "Your order shipped!") // throws if push service is down
}

class EmailNotificationHandler(private val email: EmailService) : NotificationHandler<OrderShippedNotification> {
    override suspend fun handle(notification: OrderShippedNotification) =
        email.send(notification.userId, "Your order shipped!")
}
```

```kotlin
class NotificationRegistrar(
    private val push: PushService,
    private val email: EmailService,
) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry registerNotification (
            PushNotificationHandler(push) otherwise EmailNotificationHandler(email)
        )
    }
}
```

---

## Behaviour at a glance

| Scenario                      | Result                                                 |
|-------------------------------|--------------------------------------------------------|
| First handler succeeds        | Returns immediately, rest are skipped                  |
| First throws, second succeeds | Second result returned                                 |
| All handlers throw            | Last exception re-thrown                               |
| Single `otherwise` call       | Creates a `FallbackRequestHandler` with two candidates |
| Chained `otherwise` calls     | All candidates collected into one handler — no nesting |

---

## `+` DSL shorthand

`otherwise` returns a `FallbackRequestHandler` / `FallbackNotificationHandler`, both of which implement the standard
handler interfaces, so the `+` shorthand inside a `scope { }` block works as-is:

```kotlin
registry.scope {
    +(CreateOrderHandler(api) otherwise CreateOrderFallbackHandler(cache))
    +(PushNotificationHandler(push) otherwise EmailNotificationHandler(email))
}
```

---

## Next

→ [Exceptions](exceptions.md) — per-request exception handlers that transform errors into responses
