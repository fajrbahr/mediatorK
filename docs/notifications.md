# Notifications

A **notification** is a broadcast event: the publisher fires it and doesn't care who — or how many — handlers react. Zero handlers is fine; no exception is thrown.

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
class AnalyticsHandler : NotificationHandler<BookingPurchasedNotification> {
    override suspend fun handle(notification: BookingPurchasedNotification) {
        analytics.track("purchase", notification.bookingId)
    }
}

class EmailHandler(private val mailer: Mailer) : NotificationHandler<BookingPurchasedNotification> {
    override suspend fun handle(notification: BookingPurchasedNotification) {
        mailer.sendReceipt(notification.bookingId, notification.amount)
    }
}
```

---

## Registering handlers

```kotlin
class NotificationRegistrar(
    private val analytics: AnalyticsHandler,
    private val email: EmailHandler,
) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry registerNotification analytics
        registry registerNotification email
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

| Strategy | Behaviour |
|---|---|
| `ParallelNotificationPublisher` | All handlers run concurrently *(default)* |
| `SequentialNotificationPublisher` | Handlers run one-by-one; stops on first error |
| `ContinueOnExceptionNotificationPublisher` | All handlers run even if some fail; errors collected into `AggregateException` |
| `FireAndForgetNotificationPublisher` | Returns immediately; handlers run in the background |

```kotlin
val mediator = MediatorFactory.create(
    registrars = listOf(NotificationRegistrar(analytics, email)),
    notificationPublisher = ContinueOnExceptionNotificationPublisher(),
)
```

---

## Next

→ [Pipeline Behaviors](pipeline.md)
