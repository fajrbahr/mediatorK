---
id: notifications
title: Notifications
sidebar_label: Notifications
---

# Notifications

A **notification** is a broadcast event: the publisher fires it and doesn't care who — or how many — handlers react.
By default, publishing a notification with no registered handlers throws `MissingNotificationHandlerException`.

Use notifications when something *happened* and other parts of the system should react independently.

---

## Defining a notification

```kotlin
data class BookingPurchasedNotification(
    val bookingId: String,
    val amount: Double,
) : Notification
```

---

## Implementing handlers

Multiple handlers can react to the same notification. Each is independent.

```kotlin
class TrackOrderAnalyticsHandler : NotificationHandler<BookingPurchasedNotification> {
    override suspend fun handle(notification: BookingPurchasedNotification) {
        analytics.track("purchase", notification.bookingId)
    }
}

class UpdateInventoryHandler(private val inventory: InventoryService) : NotificationHandler<BookingPurchasedNotification> {
    override suspend fun handle(notification: BookingPurchasedNotification) {
        inventory.decrementStock(notification.bookingId, notification.amount)
    }
}
```

---

## Registering handlers

Use `+handler` as shorthand or call `registerNotification()` directly — both are equivalent:

```kotlin
class NotificationRegistrar : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.scope {
            +TrackOrderAnalyticsHandler()
            registerNotification(UpdateInventoryHandler(inventoryService))
        }
    }
}
```

---

## Publishing

```kotlin
mediator.publish(BookingPurchasedNotification(bookingId = "b-1", amount = 250.0))
```

---

## Publish strategies

Control how handlers are invoked by passing a `NotificationPublisher` to `MediatorFactory.create`:

| Strategy                                   | Behaviour                                                                      |
|--------------------------------------------|--------------------------------------------------------------------------------|
| `ParallelNotificationPublisher`            | All handlers run concurrently *(default)*                                      |
| `SequentialNotificationPublisher`          | Handlers run one-by-one; stops on first error                                  |
| `ContinueOnExceptionNotificationPublisher` | All handlers run even if some fail; errors collected into `AggregateException` |
| `FireAndForgetNotificationPublisher`       | Returns immediately; handlers run in the background                            |

```kotlin
val mediator = MediatorFactory.create(
    registrars = listOf(NotificationRegistrar(analytics, email)),
    notificationPublisher = ContinueOnExceptionNotificationPublisher(),
)
```

---

## Missing handler

Control what happens when a notification is published with no registered handlers via
`missingNotificationHandler` in `MediatorFactory.create`.

| Implementation                      | Behaviour                                                   |
|-------------------------------------|-------------------------------------------------------------|
| `ThrowMissingNotificationHandler`   | Throws `MissingNotificationHandlerException` *(default)*    |
| `SilentMissingNotificationHandler`  | Drops the notification silently                             |
| Your own implementation             | Anything — dead-letter queue, logging, alerting, etc.       |

```kotlin
// default — throws if no handler is registered
val mediator = MediatorFactory.create(
    registrars = listOf(AppRegistrar()),
    missingNotificationHandler = ThrowMissingNotificationHandler(),
)

// silent — notification dropped with no error
val mediator = MediatorFactory.create(
    registrars = listOf(AppRegistrar()),
    missingNotificationHandler = SilentMissingNotificationHandler(),
)

// custom — dead-letter queue, logging, alerting
val mediator = MediatorFactory.create(
    registrars = listOf(AppRegistrar()),
    missingNotificationHandler = DeadLetterNotificationHandler(queue, logger),
)
```

:::danger
`SilentMissingNotificationHandler` silently drops notifications. Only use it when
unhandled notifications are intentional — misconfiguration will produce no error and
no trace, making it very hard to debug.
:::

The parameter type is `NotificationHandler<Notification>` — the same interface you already
use for regular handlers. Implement it directly for a custom behavior:

```kotlin
class DeadLetterNotificationHandler(
    private val queue: DeadLetterQueue,
    private val logger: Logger,
) : NotificationHandler<Notification> {
    override suspend fun handle(notification: Notification) {
        logger.warn("No handler for ${notification::class.simpleName}")
        queue.enqueue(notification)
    }
}
```

---

## Fallback chain

Register a fallback chain for a single notification slot with the `otherwise` infix operator. The first handler that
succeeds wins — the rest are skipped.

```kotlin
registry registerNotification (
    PushNotificationHandler(push) otherwise EmailNotificationHandler(email)
)
```

→ See [Fallback Chains](fallback.md) for the full guide.

---

## Next

→ [Pipeline Behaviors](pipeline.md)
