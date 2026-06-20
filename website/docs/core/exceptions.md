---
id: exceptions
title: Exception Handling
sidebar_label: Exception Handling
---

# Exception Handling

MediatorK provides two layers of exception handling:

1. **`ErrorTrackingPipelineBehavior`** — intercept every unhandled exception in the pipeline and forward it to a
   callback (e.g. crash reporting) before rethrowing.
2. **`AggregateException`** — thrown by `ContinueOnExceptionNotificationPublisher` when multiple notification handlers
   fail.

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

Use `order = Int.MAX_VALUE` (the default) so the tracker fires after retry and timeout behaviors have already given up.

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

---

## Next

→ [Validation](validation.md)
