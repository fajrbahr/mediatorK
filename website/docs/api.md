---
id: api
title: API Reference
sidebar_label: API Reference
---

# API Reference

Quick reference for all public types in `com.fajrbahr.mediatork`.

---

## Core interfaces

### `Request<out TResponse>`
Marker interface for requests. Implement to declare a message that expects exactly one handler.

```kotlin
interface Request<out TResponse>
```

**Nested type:**  
`Request.Unit` — convenience marker for commands that return no value. Equivalent to `Request<Unit>`.

---

### `RequestHandler<TRequest, TResult>`
Handles a specific `Request` type.

```kotlin
interface RequestHandler<in TRequest : Request<TResult>, TResult> {
    suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: TRequest): TResult
}
```

---

### `Notification`
Marker interface for broadcast events with no response.

```kotlin
interface Notification
```

---

### `NotificationHandler<T>`
Reacts to a `Notification`. Multiple handlers per notification type are allowed.

```kotlin
interface NotificationHandler<in T : Notification> {
    suspend fun handle(notification: T)
}
```

---

### `PipelineBehavior`
Cross-cutting decorator that wraps each request pipeline.

| Member | Type | Default | Description |
|---|---|---|---|
| `order` | `Int` | `0` | Position in chain; lower = outermost |
| `isEnabled` | `Boolean` | `true` | Skip entirely when `false` |
| `appliesTo(request)` | `Boolean` | `true` | Opt out for specific request types |
| `process(ctx, next, req)` | `suspend TResult` | — | Core implementation; must call `next(request)` to continue |

---

### `RequestPreProcessor`
Hook that runs before the handler.

```kotlin
interface RequestPreProcessor {
    val order: Int get() = 0
    suspend fun process(requestContext: RequestContext, request: Request<*>)
}
```

---

### `RequestPostProcessor`
Hook that runs after the handler.

```kotlin
interface RequestPostProcessor {
    val order: Int get() = 0
    suspend fun process(requestContext: RequestContext, request: Request<*>, response: Any?)
}
```

---

### `RequestExceptionHandler<TRequest, TResponse, TException>`
Converts a specific exception into a valid response.

```kotlin
interface RequestExceptionHandler<in TRequest, TResponse, in TException : Throwable> {
    suspend fun handle(requestContext: RequestContext, request: TRequest, exception: TException): TResponse
}
```

---

## Registry & factory

### `HandlerRegistry`
Stores all registered handlers. Populated by `MediatorRegistrar` implementations.

| Method | Description |
|---|---|
| `register(handler)` | Register a request handler (infix, reified) |
| `registerNotification(handler)` | Register a notification handler (infix, reified) |
| `registerExceptionHandler(reqClass, exClass, handler)` | Register an exception handler |
| `scope { }` | Group registrations for readability |
| `+handler` | Operator shorthand for `register` / `registerNotification` inside `scope` |
| `hasHandler(requestType)` | Returns `true` if a handler is registered for the given request type |
| `registeredRequestTypes()` | Returns the set of all request types that have a registered handler |

---

### `MediatorFactory`

```kotlin
object MediatorFactory {
    fun create(
        registrars: List<MediatorRegistrar> = emptyList(),
        pipelineBehaviors: List<PipelineBehavior> = emptyList(),
        preProcessors: List<RequestPreProcessor> = emptyList(),
        notificationPublisher: NotificationPublisher = ParallelNotificationPublisher(),
        postProcessors: List<RequestPostProcessor> = emptyList(),
    ): Mediator
}
```

| Parameter | Default | Description |
|---|---|---|
| `registrars` | `emptyList()` | Modules that contribute handlers to the registry |
| `pipelineBehaviors` | `emptyList()` | Cross-cutting decorators; sorted by `order` |
| `preProcessors` | `emptyList()` | Hooks that run before the handler; sorted by `order` |
| `notificationPublisher` | `ParallelNotificationPublisher()` | Strategy for delivering notifications |
| `postProcessors` | `emptyList()` | Hooks that run after the handler; sorted by `order` |

---

### `MediatorRegistrar`
Contributes handlers to the registry at startup.

```kotlin
interface MediatorRegistrar {
    fun register(registry: HandlerRegistry)
}
```

---

## Mediator interface

```kotlin
interface Mediator : Sender, Publisher
```

| Method | Description |
|---|---|
| `send(request)` | Dispatch a request; returns `TResponse`. Throws `MissingHandlerException` if no handler. |
| `publish(notification)` | Broadcast a notification to all registered handlers. |

---

## Notification publishers

| Class | Behaviour |
|---|---|
| `ParallelNotificationPublisher` | All handlers run concurrently *(default)* |
| `SequentialNotificationPublisher` | Handlers run one-by-one; stops on first error |
| `ContinueOnExceptionNotificationPublisher` | All handlers run; errors collected into `AggregateException` |
| `FireAndForgetNotificationPublisher` | Returns immediately; handlers run in the background |

---

## Exceptions

| Class | Description |
|---|---|
| `MediatorException` | Base class for all MediatorK errors |
| `MissingHandlerException` | No handler registered for the dispatched request type |
| `AggregateException` | One or more notification handlers failed |

---

## Validator package (`com.fajrbahr.mediatork.validator`)

| Type | Description |
|---|---|
| `RequestValidator<T>` | Validates a request; returns `ValidationResult` |
| `ValidationResult` | Holds zero or more `ValidationError`s; `isValid` when empty |
| `ValidationError` | A single failure with an optional `FieldValidator` field and a message |
| `FieldValidator` | Marker interface for typed field identifiers |
| `DefaultField` | Sentinel for errors not tied to a specific field |
| `ValidationBehavior` | Pre-built `PipelineBehavior` that runs validators and throws `ValidationException` on failure |
| `ValidationException` | Thrown when validation fails; carries the list of `ValidationError`s |
| `rules { }` | DSL builder — evaluates all rules and collects every error |
| `rulesFailFast { }` | DSL builder — stops at the first error |
