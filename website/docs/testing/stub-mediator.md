---
id: stub-mediator
title: StubMediator
sidebar_label: StubMediator
---

# StubMediator

`StubMediator` is a lightweight stub with a clean DSL — no handler classes, no registry. Declare what each request returns and go.

Best for **ViewModel tests** and **integration tests** where you control every response.

---

## Request stubs

```kotlin
val mediator = StubMediator()

mediator.on<GetUserQuery>() returns UserModel("Alice")
mediator.on<CreateOrderCommand>() throws IllegalStateException("out of stock")
mediator.on<GetUserQuery> { query -> UserModel("User-${query.id}") }
```

### With a validator

Pass a `RequestValidator` to run validation before the handler lambda:

```kotlin
mediator.on<CreateOrderCommand>(CreateOrderValidator()) { cmd ->
    OrderResult(cmd.itemId)
}
```

If validation fails, `send` throws `ValidationException` — exactly like the real pipeline.

`send` throws if no stub is registered for the request type.

---

## Notification stubs

```kotlin
mediator.onNotification<OrderPlacedEvent>() answers { event -> log(event.orderId) }
mediator.onNotification<OrderPlacedEvent>() throws RuntimeException("fail")
```

Unstubbed notifications are silently ignored.

---

## Stream stubs

```kotlin
mediator.onStream<StreamItemsQuery>() returns listOf("a", "b", "c")
mediator.onStream<StreamItemsQuery>() throws IOException("disconnected")
mediator.onStream<StreamItemsQuery>() answers { query ->
    flow { emit("${query.prefix}-1") }
}
```

Unstubbed streams return an empty `Flow`.

---

## Pipeline behaviors

Wrap a real `PipelineBehavior` and control it per-stub:

```kotlin
val stub = mediator.onPipeline(LoggingBehavior())
stub.enabled = true   // default
stub.order = 0        // inherited from the behavior
```

Global toggle:

```kotlin
mediator.pipelineEnabled = false   // skip all behaviors
```

---

## When to use what

| Helper         | Use when…                                                  |
|----------------|-------------------------------------------------------------|
| `StubMediator` | You want a one-liner DSL to control responses. No wiring.  |
| `FakeMediator` | You need the real pipeline with real handler classes.        |
| `DummyMediator` | Constructor needs a `Mediator` but `send` is never called. |
| `MediatorSpy`  | You want to assert *which* requests were dispatched.        |
