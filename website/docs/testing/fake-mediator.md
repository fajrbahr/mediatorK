---
id: fake-mediator
title: FakeMediator & Test Helpers
sidebar_label: FakeMediator
---

# FakeMediator & Test Helpers

`mediatork-test` ships a set of test helpers that let you write handler and ViewModel tests without a mocking library.

---

## Testing without a mocking library

The biggest testing win from MediatorK is what it does to your ViewModel constructor.

A typical ViewModel that manages its own dependencies directly ends up looking like this:

```kotlin
class SplashViewModel(
    private val appInfo: AppInfo,
    private val fetchAndCacheFeaturesFlagsUseCase: FetchAndCacheFeaturesFlagsUseCase,
    observeFeatureFlagsUseCase: ObserveFeatureFlagsUseCase,
    private val saveCacheDataUseCase: SaveCacheDataUseCase,
    private val getCurrentUserAndCacheUseCase: GetCurrentUserAndCacheUseCase,
    getPrefLanguageUseCase: GetPrefLanguageUseCase,
    getThemeConfigUseCase: GetThemeConfigUseCase,
    private val analyticsTrackerPort: AnalyticsTrackerPort,
    val environmentConfiguration: EnvironmentConfig,
    val performanceTracker: PerformanceTracker,
    val firebasePerformanceTracker: TraceListener,
    val basicLoggerTracker: BasicLoggerTracker,
) : ViewModel()
```

To instantiate this in a test you need to stub or mock every one of those twelve parameters — even if the test only exercises one code path that touches two of them. Every new use-case added to the ViewModel breaks every existing test that constructs it.

With MediatorK the constructor collapses to one dependency:

```kotlin
class SplashViewModel(
    private val mediator: Mediator,
) : ViewModel()
```

Now every test starts the same way:

```kotlin
val vm = SplashViewModel(DummyMediator())      // no send calls in this test
val vm = SplashViewModel(FakeMediator())       // register handlers as needed
```

The use-cases, analytics trackers, feature-flag observers, and performance trackers are all moved into individual `RequestHandler` implementations. Each handler is tested in isolation. The ViewModel test only verifies how the ViewModel reacts to success or failure — it never needs to know which use-cases exist.

| Helper | What it does |
|---|---|
| `FakeMediator` | Real mediator backed by a live `HandlerRegistry`. Register handlers at any time. |
| `DummyMediator` | Zero-arg no-op. `send` silently returns, `publish` does nothing. |
| `fakeHandler` | Creates a `RequestHandler` from a suspend lambda. |
| `fakeNotificationHandler` | Creates a `NotificationHandler` from a suspend lambda. |

---

## Installation

```kotlin
dependencies {
    testImplementation("io.github.fajrbahr:mediatork-test:0.1.6")
}
```

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

`send` returns `Unit` silently — no exception, no result processing. If your test does call `send` and the result matters, use `FakeMediator` instead.

---

## FakeMediator

`FakeMediator` wraps a real `HandlerRegistry` and a real mediator pipeline. It dispatches requests to whatever handlers you register, giving you the full pipeline (behaviors, pre/post processors) without a running application.

### Register handlers at construction

```kotlin
val mediator = FakeMediator {
    +CreateOrderHandler()
    +FetchUserHandler()
}
```

### Register handlers after construction

Handlers can also be added mid-test — useful for changing behaviour between calls in the same test:

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

`fakeHandler` builds a `RequestHandler` from a suspend lambda. The type arguments pin the request and result types — no anonymous object boilerplate.

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

## fakeNotificationHandler

`fakeNotificationHandler` builds a `NotificationHandler` from a suspend lambda. Register it via `mediator.registry`:

```kotlin
val captured = mutableListOf<OrderPlacedEvent>()

val mediator = FakeMediator {
    +fakeNotificationHandler<OrderPlacedEvent> { event ->
        captured += event
    }
}

mediator.publish(OrderPlacedEvent(orderId = "ORD-1"))

assertEquals(1, captured.size)
assertEquals("ORD-1", captured.first().orderId)
```

---

## ViewModel testing

Full runnable example: [`sample/src/test/kotlin/sample/OrderViewModelTest.kt`](https://github.com/fajrbahr/MediatorK/blob/main/sample/src/test/kotlin/sample/OrderViewModelTest.kt).

---

## Choosing the right helper

| Situation | Use |
|---|---|
| Test only checks initial state, never calls `send` | `DummyMediator()` |
| Test controls what `send` returns | `FakeMediator` + `fakeHandler` |
| Test verifies all handlers are wired up | [`MediatorTestUtils.assertAllHandlersRegistered`](handler-validation.md) |
| Test captures published notifications | `FakeMediator` + `fakeNotificationHandler` |
