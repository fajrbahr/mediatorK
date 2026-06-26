---
id: exceptions
title: Exception Handling
sidebar_label: Exception Handling
---

# Exception Handling

---

## Handling missing handlers

By default, `send()` for an unregistered request type throws `MissingHandlerException`.
Customize this behavior via `missingRequestHandler` in `MediatorFactory.create`:

```kotlin
// Default — throws immediately
val mediator = MediatorFactory.create(
    registrars = listOf(AppRegistrar()),
    missingRequestHandler = ThrowMissingRequestHandler(), // default
)

// Silent — returns a default value instead of throwing
val mediator = MediatorFactory.create(
    registrars = listOf(AppRegistrar()),
    missingRequestHandler = SilentMissingRequestHandler(default = null),
)
```

:::danger
`SilentMissingRequestHandler` silently drops requests. Only use it when unhandled
requests are intentional — misconfiguration will produce no error and no trace.
:::

---

## Missing notification handler

Control what happens when a notification is published with no registered handlers via
`missingNotificationHandler` in `MediatorFactory.create`.

| Implementation                     | Behaviour                                                |
|------------------------------------|----------------------------------------------------------|
| `ThrowMissingNotificationHandler`  | Throws `MissingNotificationHandlerException` *(default)* |
| `SilentMissingNotificationHandler` | Drops the notification silently                          |
| Your own implementation            | Anything — dead-letter queue, logging, alerting, etc.    |

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

## ErrorTrackingPipelineBehavior

Register this behavior to wire crash-reporting services (Firebase Crashlytics, Sentry, Bugsnag, etc.) into the pipeline
without touching handler code. The callback receives the original request and the throwable — the exception is always
rethrown after the callback returns.

```kotlin
val mediator = MediatorFactory.create(
    registrars = listOf(AppRegistrar()),
    pipelineBehaviors = listOf(
        ErrorTrackingPipelineBehavior(
            order = Int.MAX_VALUE,   // innermost by default — fires closest to the handler
            onError = { request, error ->
                Crashlytics.recordException(error)
            },
        ),
    ),
)
```

Use `order = Int.MAX_VALUE` (the default) to place the tracker innermost — it captures every exception directly from the
handler before it bubbles up through retry or timeout. If you only want to report failures after all retries are
exhausted, use an order lower than `RetryPipelineBehavior` (e.g. `Int.MIN_VALUE`) to place it outside the retry wrapper.

---

## Built-in exceptions

| Class                                 | Thrown when                                                                                            |
|---------------------------------------|--------------------------------------------------------------------------------------------------------|
| `MediatorException`                   | Base class for all MediatorK errors                                                                    |
| `MissingHandlerException`             | `send()` called for a request type with no registered handler                                          |
| `MissingStreamHandlerException`       | `stream()` called for a stream request type with no registered handler                                 |
| `MissingNotificationHandlerException` | Notification published with no registered handlers (only when using `ThrowMissingNotificationHandler`) |
| `AggregateException`                  | `ContinueOnExceptionNotificationPublisher` — one or more notification handlers failed                  |

### MissingHandlerException

```kotlin
// message includes the unresolved type AND all registered types to aid debugging:
// "No handler registered for 'DeleteUserCommand'. Registered: GetUserQuery, CreateOrderCommand"
```

### AggregateException

```kotlin
try {
    mediator.publish(SomeNotification())
} catch (e: AggregateException) {
    e.message // "2 handler(s) failed: ..."
}
```

---

## trySend — Result wrapper

Use `trySend` when you want to handle errors as `Result` instead of catching exceptions:

```kotlin
val result: Result<User> = mediator.trySend(GetUserQuery("user-1"))
result.onSuccess { user -> ... }
result.onFailure { error -> ... }
```
