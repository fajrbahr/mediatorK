---
id: api
title: API Reference
sidebar_label: API Reference
---

# API Reference

Quick reference for all public types in `com.fajrbahr.mediatork`.

| Subpackage                            | Contents                                                                                                                                                                                 |
|---------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `com.fajrbahr.mediatork`              | Core: `Mediator`, `Request`, `StreamRequest`, `HandlerRegistry`, `MediatorFactory`, `MediatorException` hierarchy                                                                        |
| `com.fajrbahr.mediatork.handler`      | `RequestHandler`, `StreamRequestHandler`, `FallbackRequestHandler` (`otherwise`), `Sender`, `trySend`                                                                                    |
| `com.fajrbahr.mediatork.notification` | `Notification`, `NotificationHandler`, `FallbackNotificationHandler` (`otherwise`), all publisher implementations, `ThrowMissingNotificationHandler`, `SilentMissingNotificationHandler` |
| `com.fajrbahr.mediatork.pipeline`     | `PipelineBehavior`, `Stage`, `StreamPipelineBehavior` and all built-in behaviors (logging, caching, timeout, timing, error tracking, request counter)                                    |
| `com.fajrbahr.mediatork.validator`    | `RequestValidator`, `ValidationBehavior`, `ValidationResult`, `ValidationException`, `rules`, `rulesFailFast`                                                                            |

---

## Core interfaces

### `Request<out TResponse>`

Marker interface for requests. Implement to declare a message that expects exactly one handler.

```kotlin
interface Request<out TResponse>
```

**Nested type:**  
`Request.Unit`: convenience marker for commands that return no value. Equivalent to `Request<Unit>`.

---

### `RequestHandler<TRequest, TResult>` · `com.fajrbahr.mediatork.handler`

Handles a specific `Request` type.

```kotlin
interface RequestHandler<in TRequest : Request<TResult>, TResult> {
    suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: TRequest): TResult
}
```

---

### `StreamRequest<T>`

Marker interface for requests that return a lazy `Flow<T>` instead of a single value. Dispatch via `Mediator.stream()`.

```kotlin
interface StreamRequest<out T>
```

Use when the response is a sequence produced over time: large result sets, live feeds, cursor-based exports, or
anything better consumed incrementally than batched into a list.

---

### `StreamRequestHandler<TRequest, T>` · `com.fajrbahr.mediatork.handler`

Handles a `StreamRequest` and returns a cold `Flow<T>`. The interface is **not** `suspend`; it returns the flow
immediately; work begins when the caller collects it.

```kotlin
interface StreamRequestHandler<in TRequest : StreamRequest<T>, T> {
    fun handle(mediator: Mediator, requestContext: RequestContext, request: TRequest): Flow<T>
}
```

```kotlin
// Define
data class StreamInvoicesQuery(val status: InvoiceStatus? = null) : StreamRequest<Invoice>

// Handle
class StreamInvoicesHandler(private val repo: InvoiceRepository)
    : StreamRequestHandler<StreamInvoicesQuery, Invoice> {
    override fun handle(mediator: Mediator, requestContext: RequestContext, request: StreamInvoicesQuery): Flow<Invoice> =
        repo.all().asFlow().let { flow ->
            if (request.status != null) flow.filter { it.status == request.status } else flow
        }
}

// Dispatch
mediator.stream(StreamInvoicesQuery(status = InvoiceStatus.APPROVED)).collect { invoice -> ... }
```

---

### `IStreamRequest`

Capability for dispatching a `StreamRequest` to its handler. Implemented by `Mediator`.

```kotlin
interface IStreamRequest {
    fun <TRequest : StreamRequest<T>, T> stream(request: TRequest): Flow<T>
}
```

`stream()` is non-suspend. It resolves the handler and returns a cold `Flow` immediately. Each collection starts a fresh
`RequestContext`.

---

### `Notification` · `com.fajrbahr.mediatork.notification`

Marker interface for broadcast events with no response.

```kotlin
interface Notification
```

---

### `NotificationHandler<T>` · `com.fajrbahr.mediatork.notification`

Reacts to a `Notification`. Multiple handlers per notification type are allowed.

```kotlin
interface NotificationHandler<in T : Notification> {
    suspend fun handle(notification: T)
}
```

---

### `PipelineBehavior` · `com.fajrbahr.mediatork.pipeline`

Cross-cutting decorator that wraps each request pipeline.

| Member                    | Type              | Default         | Description                                                          |
|---------------------------|-------------------|-----------------|----------------------------------------------------------------------|
| `stage`                   | `Stage`           | `Stage.Default` | Absolute position group: `Pre`, `Default`, or `Post`                 |
| `order`                   | `Int`             | `0`             | Position within the stage; lower = outermost                         |
| `isEnabled`               | `Boolean`         | `true`          | Skip entirely when `false`                                           |
| `appliesTo(request)`      | `Boolean`         | `true`          | Opt out for specific request types                                   |
| `process(ctx, next, req)` | `suspend TResult` | required        | Core implementation; must call `next(request)` to continue the chain |

### `Stage`

Controls the absolute position of a `PipelineBehavior` in the chain.

| Value           | Position           |
|-----------------|--------------------|
| `Stage.Pre`     | Outermost wrappers |
| `Stage.Default` | Middle *(default)* |
| `Stage.Post`    | Innermost wrappers |

Stage always beats `order`. See [Pre / Post Behaviors](core/processors.md).

---

## Registry & factory

### `HandlerRegistry`

Stores all registered handlers. Populated by `MediatorRegistrar` implementations.

| Method                            | Description                                                               |
|-----------------------------------|---------------------------------------------------------------------------|
| `register(handler)`               | Register a request handler (infix, reified)                               |
| `registerStream(handler)`         | Register a stream request handler (infix, reified)                        |
| `registerNotification(handler)`   | Register a notification handler (infix, reified)                          |
| `registerValidator(validator)`    | Register a request validator (reified)                                    |
| `registerDynamic(klass, handler)` | Register a request handler without reified type (for DI frameworks)       |
| `scope { }`                       | Group registrations for readability                                       |
| `+handler`                        | Operator shorthand for `register` / `registerNotification` inside `scope` |
| `hasHandler(requestType)`         | Returns `true` if a handler is registered for the given request type      |
| `registeredRequestTypes()`        | Returns the set of all request types that have a registered handler       |

---

### `MediatorFactory`

```kotlin
object MediatorFactory {
    fun create(
        registrars: List<MediatorRegistrar> = emptyList(),
        pipelineBehaviors: List<PipelineBehavior> = emptyList(),
        streamPipelineBehaviors: List<StreamPipelineBehavior> = emptyList(),
        notificationPublisher: NotificationPublishStrategy = ParallelNotificationPublisher(),
        verifyHandlers: Boolean = true,
        missingNotificationHandler: NotificationHandler<Notification> = ThrowMissingNotificationHandler(),
        missingRequestHandler: RequestHandler<Request<Any?>, Any?> = ThrowMissingRequestHandler(),
    ): Mediator
}
```

| Parameter                    | Default                             | Description                                                                                      |
|------------------------------|-------------------------------------|--------------------------------------------------------------------------------------------------|
| `registrars`                 | `emptyList()`                       | Modules that contribute handlers to the registry                                                 |
| `pipelineBehaviors`          | `emptyList()`                       | Cross-cutting decorators; grouped by `Stage` then sorted by `order` within each group            |
| `streamPipelineBehaviors`    | `emptyList()`                       | Cross-cutting decorators for `StreamRequest` dispatches; sorted by `order`                       |
| `notificationPublisher`      | `ParallelNotificationPublisher()`   | Strategy for delivering notifications                                                            |
| `verifyHandlers`             | `true`                              | When `true`, logs a warning for every request type with no handler after all registrars have run |
| `missingNotificationHandler` | `ThrowMissingNotificationHandler()` | What to do when a notification is published with no registered handlers                          |
| `missingRequestHandler`      | `ThrowMissingRequestHandler()`      | What to do when `send()` is called for an unregistered request type                              |

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
interface Mediator : Sender, IStreamRequest, Publisher
```

| Method                             | Description                                                                                                 |
|------------------------------------|-------------------------------------------------------------------------------------------------------------|
| `send(request)`                    | Dispatch a `Request`; returns `TResponse`. Throws `MissingHandlerException` if no handler.                  |
| `stream(request)`                  | Dispatch a `StreamRequest`; returns a cold `Flow<T>`. Throws `MissingStreamHandlerException` if no handler. |
| `publish(notification)`            | Broadcast a notification using the default `NotificationPublisher`.                                         |
| `publish(notification, publisher)` | Broadcast a notification using the supplied publisher, overriding the default for this call only.           |

---

## Notification publishers · `com.fajrbahr.mediatork.notification`

| Class                                      | Behaviour                                                    |
|--------------------------------------------|--------------------------------------------------------------|
| `ParallelNotificationPublisher`            | All handlers run concurrently *(default)*                    |
| `SequentialNotificationPublisher`          | Handlers run one-by-one; stops on first error                |
| `ContinueOnExceptionNotificationPublisher` | All handlers run; errors collected into `AggregateException` |
| `FireAndForgetNotificationPublisher`       | Returns immediately; handlers run in the background          |

---

## Exceptions

| Class                                 | Description                                                            |
|---------------------------------------|------------------------------------------------------------------------|
| `MediatorException`                   | Base class for all MediatorK errors                                    |
| `MissingHandlerException`             | No handler registered for the dispatched request type                  |
| `MissingStreamHandlerException`       | No stream handler registered for the dispatched `StreamRequest` type   |
| `MissingNotificationHandlerException` | No handlers registered for a published notification type               |
| `AggregateException`                  | One or more notification handlers failed (from `ContinueOnException…`) |

---

## Validator package (`com.fajrbahr.mediatork.validator`)

| Type                  | Description                                                                                                                  |
|-----------------------|------------------------------------------------------------------------------------------------------------------------------|
| `RequestValidator<T>` | Validates a request and returns `ValidationResult`. Register via `registry.registerValidator(validator)`.                    |
| `ValidationResult`    | Sealed class: `Valid` or `Invalid(errors: List<*>)`                                                                          |
| `ValidationBehavior`  | Pre-built `PipelineBehavior` (order `-50`) that runs registered validators before the handler                                |
| `ValidationException` | Thrown when validation fails; `errors: List<*>` carries all failure messages (any type: `String`, sealed class, enum, etc.) |
| `rules { }`           | Collect-all DSL; all `check`/`require` calls run, errors accumulated                                                        |
| `rulesFailFast { }`   | Stop-on-first DSL; execution stops at the first failing `check`/`require`                                                   |
| `throwIfInvalid()`    | Extension on `ValidationResult`; throws `ValidationException` if the result is `Invalid`                                    |

