---
id: dump-mediator
title: DummyMediator
sidebar_label: DummyMediator
---

# DummyMediator

`DummyMediator` is a no-op `Mediator` included in `mediatork-test` for use in tests.

- `publish` does nothing — fire and forget, no handlers called.
- `send` returns silently — no exception, no result processing.

No fake class to write, no mocking library needed.

---

## Usage

Use it when a test needs a `Mediator` to satisfy a constructor but never actually calls `send`:

```kotlin
val vm = OrderViewModel(DummyMediator())
```

To simulate a failure, use `FakeMediator` with a `fakeHandler` that throws:

```kotlin
val mediator = FakeMediator()
mediator.register(fakeHandler<CreateOrderCommand, OrderResult> { _, _, _ ->
    throw RuntimeException("Network unavailable")
})
```

To capture what was sent, use `FakeMediator` with a `fakeHandler` that records:

```kotlin
val captured = mutableListOf<Any>()

val mediator = FakeMediator()
mediator.register(fakeHandler<CreateOrderCommand, OrderResult> { _, _, request ->
    captured += request
    OrderResult(orderId = request.id)
})
```

---

## Import

```kotlin
import com.fajrbahr.mediatork.test.DummyMediator
```
