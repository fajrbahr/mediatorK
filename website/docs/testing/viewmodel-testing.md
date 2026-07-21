---
id: viewmodel-testing
title: Testing ViewModels
sidebar_label: Testing ViewModels
---

# Testing ViewModels

Create a fresh `testMediator` and `ViewModel` inside each test. Register the handler body the test needs, drive the
ViewModel, then assert:

```kotlin
@Test
fun `createOrder success updates stateFlow`() = runTest {
    val mediator = testMediator {
        handle<CreateOrderCommand, OrderResult> { OrderResult(orderId = "ORD-1", responseTime = 10) }
    }
    val vm = OrderViewModel(mediator)

    vm.createOrder("ORD-1", 99.0)
    advanceUntilIdle()

    val state = vm.stateFlow.value
    assertEquals("ORD-1", state.orderResult?.orderId)
    assertFalse(state.isLoading)
    assertNull(state.error)
}

@Test
fun `createOrder failure sets error`() = runTest {
    val mediator = testMediator {
        handle<CreateOrderCommand, OrderResult> { throw RuntimeException("Network unavailable") }
    }
    val vm = OrderViewModel(mediator)

    vm.createOrder("1", 99.0)
    advanceUntilIdle()

    assertEquals("Network unavailable", vm.stateFlow.value.error)
    assertNull(vm.stateFlow.value.orderResult)
}

@Test
fun `initial state is empty and not loading`() {
    val vm = OrderViewModel(testMediator { }) // real empty mediator — the test never calls send
    assertEquals(OrderUiState(), vm.stateFlow.value)
}
```

Need to change a handler's behaviour between calls in the same test? Build a fresh `testMediator { }` for each phase, or
keep a mutable response the handler reads:

```kotlin
@Test
fun `error is cleared on next call`() = runTest {
    var fail = true
    val mediator = testMediator {
        handle<CreateOrderCommand, OrderResult> {
            if (fail) throw RuntimeException("first failure") else OrderResult(orderId = "ORD-2")
        }
    }
    val vm = OrderViewModel(mediator)

    vm.createOrder("1", 10.0); advanceUntilIdle()
    assertNotNull(vm.stateFlow.value.error)

    fail = false
    vm.createOrder("2", 20.0); advanceUntilIdle()
    assertNull(vm.stateFlow.value.error)
    assertEquals("ORD-2", vm.stateFlow.value.orderResult?.orderId)
}
```

To assert *which* commands the ViewModel dispatched, read the recording — `mediator.sentOf<CreateOrderCommand>()`. See
[testMediator](test-mediator.md#recordingmediator).

---

## Next

→ [Troubleshooting & FAQ](../troubleshooting.md)
