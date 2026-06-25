---
id: the-promise
title: The Promise
sidebar_label: The Promise
---

# The Promise

> **From a ViewModel with 12 constructor parameters — down to one. And tests that cover 70 %+ of your business logic without a single mock.**

:::tip ViewModel Weight-Loss Program™
**Diet your fat ViewModel — results in one week.**
Before: 12 constructor parameters, 40 lines of mock setup, a test suite that dreads every refactor.
After: 1 dependency, 5-line tests, zero mocking libraries.
*Not a real program. Typical results achieved in the first PR.*
:::

This page shows the two concrete promises MediatorK makes:

1. Your ViewModel becomes a thin dispatcher with exactly **one** dependency.
2. Every piece of logic moves into a focused handler that is **testable in isolation**, using real implementations instead of mocks.

---

## The Problem — an XXL ViewModel

Real-world ViewModels tend to grow. Each new feature pulls in another dependency until the constructor looks like this:

```kotlin
class InitialViewModel(
    private val applicationMetadata: ApplicationMetadata,
    private val retrieveAndStoreTogglesUseCase: RetrieveAndStoreTogglesUseCase,
    watchTogglesUseCase: WatchTogglesUseCase,
    private val persistCachedInfoUseCase: PersistCachedInfoUseCase,
    private val fetchActiveUserAndStoreUseCase: FetchActiveUserAndStoreUseCase,
    fetchPreferredLocaleUseCase: FetchPreferredLocaleUseCase,
    fetchVisualThemeUseCase: FetchVisualThemeUseCase,
    private val metricsReporterPort: MetricsReporterPort,
    val runtimeSettings: RuntimeSettings,
    val speedMonitor: SpeedMonitor,
    val cloudPerformanceTracker: PerformanceTraceListener,
    val simpleLoggingTracker: SimpleLoggingTracker,
) : ViewModel()
```

Twelve dependencies. Testing this requires constructing or mocking all twelve — even for a test that only cares about one use-case. The mock setup often dwarfs the actual test logic.

---

## The Solution — an XXS ViewModel

With MediatorK the ViewModel has exactly one dependency:

```kotlin
class OrderViewModel(private val mediator: Mediator) : ViewModel() {

    fun createOrder(id: String, amount: Double) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val result = mediator.send(CreateOrderCommand(id = id, amount = amount))
                _state.update { it.copy(orderResult = result, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}
```

Every action becomes a `mediator.send(...)` call. The ViewModel no longer knows which use-case, repository, or data source handles the request — it just dispatches.

---

## The Testing Story

Reducing the ViewModel to one dependency changes how you test it. Instead of constructing or mocking all twelve real dependencies, you swap in a single `FakeMediator` and register whatever handler the test needs:

```kotlin
// Before — mock twelve dependencies just to test one scenario
@Test
fun `place order - notifies user on success`() {
    val notificationService = mockk<NotificationService>()
    val inventoryRepo = mockk<InventoryRepository>()
    val orderRepo = mockk<OrderRepository>()
    val paymentGateway = mockk<PaymentGateway>()
    // … 8 more mocks, 12 stubs …

    val vm = OrderViewModel(notificationService, inventoryRepo, orderRepo, paymentGateway, …)
    vm.placeOrder(cart)

    verify { notificationService.notify(match { it.type == "ORDER_PLACED" }) }
}
```

```kotlin
// After — one fake, zero mocking library
@Test
fun `createOrder success updates state with result`() = runTest {
    val fakeMediator = FakeMediator()
    fakeMediator.register(
        fakeHandler<CreateOrderCommand, OrderResult> { _, _, _ ->
            OrderResult(orderId = "ORD-1", responseIme = 10)
        }
    )
    val vm = OrderViewModel(fakeMediator)

    vm.createOrder("1", 99.0)
    advanceUntilIdle()

    assertEquals("ORD-1", vm.stateFlow.value.orderResult?.orderId)
    assertFalse(vm.stateFlow.value.isLoading)
}
```

The ViewModel test does not import a mocking library. It does not know about repositories or services. It tests exactly one thing: how the ViewModel reacts to a mediator response.

---

## Handlers Are Pure Functions

Handlers have no global state. They receive a request, do their work, and return a result. That makes them trivially testable with `buildHandlerTestHarness` — a helper that wires a real mediator with real handlers but no production infrastructure:

```kotlin
@Test
fun `approved invoice transitions to APPROVED`() = runTest {
    val repo = InvoiceRepository()          // real in-memory repo
    val harness = buildHandlerTestHarness {
        +CreateInvoiceHandler(repo, CreateInvoiceDomainValidator(repo), CreateInvoicePersistenceValidator(repo))
        +ApproveInvoiceHandler(repo)
        +GetInvoiceHandler(repo)
    }

    harness.given(CreateInvoiceCommand(id = "INV-200", amount = 500.0))
    harness.send(ApproveInvoiceCommand(id = "INV-200"))

    val invoice = harness.query(GetInvoiceQuery(id = "INV-200"))
    assertEquals(InvoiceStatus.APPROVED, invoice.status)
}
```

No mocking. No `every { ... } returns`. Just real objects exercising the real path.

---

## What 66 % Coverage Looks Like

The sample project ships with **24 tests across 4 test files** and achieves **71 %+ line coverage** of business logic with zero mocking libraries.

The tests cover:

| Test class | What it tests |
|---|---|
| `InvoiceIntegrationTest` | Full invoice slice — create, approve, validate, stream, rollback |
| `OrderViewModelTest` | ViewModel happy path, error path, loading state, event emission |
| `BehaviorTest` | Retry, RateLimit, CircuitBreaker, Deduplication, Authorization |
| `SampleHandlerTest` | Basic handler stubs, notification fan-out, pipeline behaviors |
| `HandlerTest` | FetchBookings validation, ShipOrder fallback chain |
| `HandlerCoverageTest` | Registration completeness — every handler is wired |

The only files excluded from the coverage count are the `Main.kt` demo scenarios (integration scripts run manually from the IDE, not unit tests) and the Spring annotation stubs.

---

## Summary

| | Before | After |
|---|---|---|
| ViewModel constructor params | 12 | 1 |
| Mocking library required | Yes | No |
| Test setup lines per test | 20 – 40 | 3 – 8 |
| Coverage target achievable | Hard | Straightforward |

Next, see how this maps onto [Vertical Slice Architecture →](vertical-slice.md)
