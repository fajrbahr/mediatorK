# Exception Handling

MediatorK provides two layers of exception handling:

1. **`RequestExceptionHandler`** — intercept a specific exception for a specific request type and convert it into a valid response instead of propagating it.
2. **`AggregateException`** — thrown by `ContinueOnExceptionNotificationPublisher` when multiple notification handlers fail.

---

## RequestExceptionHandler

Register one to translate domain exceptions into typed responses:

```kotlin
class UserNotFoundExceptionHandler
    : RequestExceptionHandler<GetUserQuery, User?, UserNotFoundException> {

    override suspend fun handle(
        requestContext: RequestContext,
        request: GetUserQuery,
        exception: UserNotFoundException,
    ): User? = null   // return null instead of crashing
}
```

### Registering

```kotlin
registry.registerExceptionHandler(
    requestClass   = GetUserQuery::class,
    exceptionClass = UserNotFoundException::class,
    handler        = UserNotFoundExceptionHandler(),
)
```

### Matching rules

- Only one exception handler per `(request type, exception type)` combination is used — the first registered handler whose exception class `isInstance` of the thrown exception wins.
- If no handler matches, the exception propagates normally.

---

## Built-in exceptions

| Class | Thrown when |
|---|---|
| `MediatorException` | Base class for all MediatorK errors |
| `MissingHandlerException` | `send()` called for a request type with no registered handler |
| `AggregateException` | `ContinueOnExceptionNotificationPublisher` — one or more notification handlers failed |

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

## Next

→ [Validation](validation.md)
