---
id: fake-mediator
title: FakeMediator & Test Helpers
sidebar_label: FakeMediator
---

# FakeMediator & Test Helpers

`mediatork-test` ships a set of test helpers that let you write handler and ViewModel tests without a mocking library.

| Helper                    | What it does                                                                     |
|---------------------------|----------------------------------------------------------------------------------|
| `FakeMediator`            | Real mediator backed by a live `HandlerRegistry`. Register handlers at any time. |
| `DummyMediator`           | Zero-arg no-op. `send` silently returns, `publish` does nothing.                 |
| `MediatorSpy`             | Wraps any mediator and records every `send` and `publish` call.                  |
| `fakeHandler`             | Creates a `RequestHandler` from a suspend lambda.                                |
| `fakeNotificationHandler` | Creates a `NotificationHandler` from a suspend lambda.                           |
| `captureNotifications`    | Registers a notification handler and returns the live captured list.             |

---

## DummyMediator

Use `DummyMediator` when a test needs a `Mediator` instance to satisfy a constructor but never actually calls `send`.

```kotlin
@Test
fun `initial state is empty and not loading`() {
    val vm = OrderViewModel(DummyMediator())
    assertEquals(OrderUiState(), vm.stateFlow.value)
}
```

`send` returns `Unit` silently; no exception, no result processing. If your test does call `send` and the result
matters, use `FakeMediator` instead.

---

## FakeMediator

`FakeMediator` wraps a real `HandlerRegistry` and a real mediator pipeline. It dispatches requests to whatever handlers
you register, giving you the full pipeline (behaviors, pre/post processors) without a running application.

### Register handlers at construction

```kotlin
val mediator = FakeMediator {
    +CreateOrderHandler()
    +FetchUserHandler()
}
```

### Register handlers after construction

Handlers can also be added mid-test, useful for changing behavior between calls in the same test:

```kotlin
@Test
fun `error is cleared on next call`() = runTest {
    val mediator = FakeMediator()
    val vm = OrderViewModel(mediator)

    mediator.register(fakeHandler<CreateOrderCommand, OrderResult> { _, _, _ ->
        throw RuntimeException("first failure")
    })
    vm.createOrder("1", 10.0)
    advanceUntilIdle()
    assertNotNull(vm.stateFlow.value.error)

    mediator.register(fakeHandler<CreateOrderCommand, OrderResult> { _, _, _ ->
        OrderResult(orderId = "ORD-2")
    })
    vm.createOrder("2", 20.0)
    advanceUntilIdle()

    assertNull(vm.stateFlow.value.error)
    assertEquals("ORD-2", vm.stateFlow.value.orderResult?.orderId)
}
```

Calling `register` again for the same request type silently replaces the previous handler.

### With registrars and pipeline behaviors

```kotlin
val mediator = FakeMediator(
    registrars = listOf(OrderRegistrar()),
    pipelineBehaviors = listOf(LoggingBehavior()),
)
```

---

## fakeHandler

`fakeHandler` builds a `RequestHandler` from a suspend lambda. The type arguments pin the request and result types, with no
anonymous object boilerplate.

```kotlin
@Test
fun `createOrder returns expected result`() = runTest {
    val mediator = FakeMediator()
    val vm = OrderViewModel(mediator)

    mediator.register(fakeHandler<CreateOrderCommand, OrderResult> { _, _, request ->
        OrderResult(orderId = request.id)
    })

    vm.createOrder("ORD-1", 99.0)
    advanceUntilIdle()

    assertEquals("ORD-1", vm.stateFlow.value.orderResult?.orderId)
}
```

Throw from the lambda to simulate failures:

```kotlin
@Test
fun `createOrder failure sets error`() = runTest {
    val mediator = FakeMediator()
    val vm = OrderViewModel(mediator)

    mediator.register(fakeHandler<CreateOrderCommand, OrderResult> { _, _, _ ->
        throw RuntimeException("Network unavailable")
    })

    vm.createOrder("1", 99.0)
    advanceUntilIdle()

    assertEquals("Network unavailable", vm.stateFlow.value.error)
}
```

---

## Choosing the right helper

| Situation                                          | Use                                                                      |
|----------------------------------------------------|--------------------------------------------------------------------------|
| Test only checks initial state, never calls `send` | `DummyMediator()`                                                        |
| Test controls what `send` returns                  | `FakeMediator` + `fakeHandler`                                           |
| Test asserts *which* requests were sent            | [`MediatorSpy`](spy.md)                                                  |
| Test captures published notifications              | [`captureNotifications`](notification-testing.md) or `MediatorSpy`      |
| Test verifies all handlers are wired up            | [`MediatorTestUtils.assertAllHandlersRegistered`](handler-validation.md) |

---

## Next

→ [MediatorSpy](spy.md)
