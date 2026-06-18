---
id: api
title: API Reference
sidebar_label: API Reference
---

# API Reference

Quick reference for all public types in `com.fajrbahr.mediatork`.

| Subpackage                            | Contents                                                                                                                                                         |
|---------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `com.fajrbahr.mediatork`              | Core: `Mediator`, `Request`, `StreamRequest`, `HandlerRegistry`, `MediatorFactory`, processors, exceptions                                                       |
| `com.fajrbahr.mediatork.handler`      | `RequestHandler`, `StreamRequestHandler`, `FallbackRequestHandler` (`otherwise`), `RequestExceptionHandler`                                                      |
| `com.fajrbahr.mediatork.notification` | `Notification`, `NotificationHandler`, `FallbackNotificationHandler` (`otherwise`), all publisher implementations, `ThrowMissingNotificationHandler`, `SilentMissingNotificationHandler` |
| `com.fajrbahr.mediatork.pipeline`     | `PipelineBehavior` and all built-in behaviors (logging, retry, caching, auth, circuit-breaker, transaction, etc.)                                                |
| `com.fajrbahr.mediatork.validator`    | `RequestValidator`, `ValidationBehavior`, `ValidationScope`, `ValidationResult`, DSL builders                                                                    |

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

### `RequestHandler<TRequest, TResult>` · `com.fajrbahr.mediatork.handler`

Handles a specific `Request` type.

```kotlin
interface RequestHandler<in TRequest : Request<TResult>, TResult> {
    suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: TRequest): TResult
}
```

---

### `StreamRequest<T>`

Marker interface for requests that return a lazy `Flow<T>` instead of a single value. Dispatch via `Streamer.stream()`.

```kotlin
interface StreamRequest<out T>
```

Use when the response is a sequence produced over time — large result sets, live feeds, cursor-based exports, or anything better consumed incrementally than batched into a list.

---

### `StreamRequestHandler<TRequest, T>` · `com.fajrbahr.mediatork.handler`

Handles a `StreamRequest` and returns a cold `Flow<T>`. The interface is **not** `suspend` — it returns the flow immediately; work begins when the caller collects it.

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

### `Streamer`

Capability for dispatching a `StreamRequest` to its handler.

```kotlin
interface Streamer {
    fun <TRequest : StreamRequest<T>, T> stream(request: TRequest): Flow<T>
}
```

`stream()` is non-suspend. It resolves the handler and returns a cold `Flow` immediately. Each collection starts a fresh `RequestContext`.

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

| Member                    | Type              | Default | Description                                                |
|---------------------------|-------------------|---------|------------------------------------------------------------|
| `order`                   | `Int`             | `0`     | Position in chain; lower = outermost                       |
| `isEnabled`               | `Boolean`         | `true`  | Skip entirely when `false`                                 |
| `appliesTo(request)`      | `Boolean`         | `true`  | Opt out for specific request types                         |
| `process(ctx, next, req)` | `suspend TResult` | —       | Core implementation; must call `next(request)` to continue |

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

### `RequestExceptionHandler<TRequest, TResponse, TException>` · `com.fajrbahr.mediatork.handler`

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

| Method                                                 | Description                                                               |
|--------------------------------------------------------|---------------------------------------------------------------------------|
| `register(handler)`                                    | Register a request handler (infix, reified)                               |
| `registerStream(handler)`                              | Register a stream request handler                                         |
| `registerNotification(handler)`                        | Register a notification handler (infix, reified)                          |
| `registerExceptionHandler(reqClass, exClass, handler)` | Register an exception handler                                             |
| `scope { }`                                            | Group registrations for readability                                       |
| `+handler`                                             | Operator shorthand for `register` / `registerNotification` inside `scope` |
| `hasHandler(requestType)`                              | Returns `true` if a handler is registered for the given request type      |
| `registeredRequestTypes()`                             | Returns the set of all request types that have a registered handler       |

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
        verifyHandlers: Boolean = true,
        missingNotificationHandler: NotificationHandler<Notification> = ThrowMissingNotificationHandler(),
    ): Mediator
}
```

| Parameter                      | Default                              | Description                                                                                      |
|--------------------------------|--------------------------------------|--------------------------------------------------------------------------------------------------|
| `registrars`                   | `emptyList()`                        | Modules that contribute handlers to the registry                                                 |
| `pipelineBehaviors`            | `emptyList()`                        | Cross-cutting decorators; sorted by `order`                                                      |
| `preProcessors`                | `emptyList()`                        | Hooks that run before the handler; sorted by `order`                                             |
| `notificationPublisher`        | `ParallelNotificationPublisher()`    | Strategy for delivering notifications                                                            |
| `postProcessors`               | `emptyList()`                        | Hooks that run after the handler; sorted by `order`                                              |
| `verifyHandlers`               | `true`                               | When `true`, logs a warning for every request type with no handler after all registrars have run |
| `missingNotificationHandler`   | `ThrowMissingNotificationHandler()`  | What to do when a notification is published with no registered handlers                          |

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
interface Mediator : Sender, Streamer, Publisher
```

| Method                              | Description                                                                              |
|-------------------------------------|------------------------------------------------------------------------------------------|
| `send(request)`                     | Dispatch a `Request`; returns `TResponse`. Throws `MissingHandlerException` if no handler. |
| `stream(request)`                   | Dispatch a `StreamRequest`; returns a cold `Flow<T>`. Throws `MissingStreamHandlerException` if no handler. |
| `publish(notification)`             | Broadcast a notification using the default `NotificationPublisher`.                      |
| `publish(notification, publisher)`  | Broadcast a notification using the supplied publisher, overriding the default for this call only. |

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

| Class                              | Description                                                         |
|------------------------------------|---------------------------------------------------------------------|
| `MediatorException`                | Base class for all MediatorK errors                                 |
| `MissingHandlerException`          | No handler registered for the dispatched request type               |
| `MissingStreamHandlerException`    | No stream handler registered for the dispatched `StreamRequest` type |
| `MissingNotificationHandlerException` | No handlers registered for a published notification type         |
| `AggregateException`               | One or more notification handlers failed (from `ContinueOnException…`) |

---

## Validator package (`com.fajrbahr.mediatork.validator`)

| Type                  | Description                                                                                   |
|-----------------------|-----------------------------------------------------------------------------------------------|
| `RequestValidator<T>` | Validates a request; returns `ValidationResult`. Declares its `scope`.                        |
| `ValidationScope`     | `REQUEST` (pipeline, automatic) · `DOMAIN` (in handler, after load) · `PERSISTENCE` (in handler, before write) |
| `ValidationResult`    | Holds zero or more `ValidationError`s; `isValid` when empty                                   |
| `ValidationError`     | A single failure with an optional `FieldValidator` field and a message                        |
| `FieldValidator`      | Marker interface for typed field identifiers                                                  |
| `DefaultField`        | Sentinel for errors not tied to a specific field                                              |
| `ValidationBehavior`  | Pre-built `PipelineBehavior` that runs `ValidationScope.REQUEST` validators automatically    |
| `ValidationException` | Thrown when validation fails; carries the list of `ValidationError`s                          |
| `rules { }`           | DSL builder — evaluates all rules and collects every error                                    |
| `rulesFailFast { }`   | DSL builder — stops at the first error                                                        |

---

## Transaction pipeline · `com.fajrbahr.mediatork.pipeline`

### `TransactionProvider`

Abstraction over a transactional unit of work. Implement once per persistence layer and pass to `TransactionPipelineBehavior`.

```kotlin
interface TransactionProvider {
    suspend fun <T> withTransaction(block: suspend () -> T): T
}

// Room
val provider = object : TransactionProvider {
    override suspend fun <T> withTransaction(block: suspend () -> T): T =
        db.withTransaction { block() }
}

// Exposed
val provider = object : TransactionProvider {
    override suspend fun <T> withTransaction(block: suspend () -> T): T =
        newSuspendedTransaction { block() }
}
```

### `TransactionPipelineBehavior`

Wraps each matching request in a transaction. Commits on success, rolls back and rethrows on any exception.

```kotlin
TransactionPipelineBehavior(
    transactionProvider = provider,
    appliesTo = { it is Request.Unit }, // limit to write commands
    order = 0,
)
```

| Parameter             | Default    | Description                                              |
|-----------------------|------------|----------------------------------------------------------|
| `transactionProvider` | —          | Required. The unit-of-work implementation.               |
| `appliesTo`           | `{ true }` | Predicate to restrict which requests run in a transaction |
| `order`               | `0`        | Position in the behavior chain                           |
