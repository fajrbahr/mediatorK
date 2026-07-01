---
id: notifications
title: Notifications
sidebar_label: Notifications
---

# Notifications

A **notification** is a broadcast event: the publisher fires it and doesn't care who, or how many, handlers react.
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

Use `+handler` as shorthand or call `registerNotification()` directly; both are equivalent:

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

Pass a strategy directly on the call to override the default for a single publish:

```kotlin
mediator.publish(
    BookingPurchasedNotification(bookingId = "b-1", amount = 250.0),
    publisher = ContinueOnExceptionNotificationPublisher(),
)
```

---

## Publish strategies

Control how handlers are invoked by passing a `NotificationPublishStrategy` to `MediatorFactory.create`:

| Strategy                                   | Behavior                                                                       |
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

## Next

→ [Pipeline Behaviors](pipeline.md)
