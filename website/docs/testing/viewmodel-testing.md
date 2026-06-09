---
id: viewmodel-testing
title: Testing ViewModels
sidebar_label: Testing ViewModels
---

# Testing ViewModels

Create a fresh `FakeMediator` and `ViewModel` inside each test. Register the handler the test needs, drive the ViewModel, then assert:

```kotlin
@Test
fun `createOrder success updates stateFlow`() = runTest {
    val mediator = FakeMediator()
    val vm = OrderViewModel(mediator)

    mediator.register(fakeHandler<CreateOrderCommand, OrderResult> { _, _, _ ->
        OrderResult(orderId = "ORD-1", responseIme = 10)
    })

    vm.createOrder("ORD-1", 99.0)
    advanceUntilIdle()

    val state = vm.stateFlow.value
    assertEquals("ORD-1", state.orderResult?.orderId)
    assertFalse(state.isLoading)
    assertNull(state.error)
}

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
    assertNull(vm.stateFlow.value.orderResult)
}

@Test
fun `initial state is empty and not loading`() {
    val vm = OrderViewModel(DummyMediator())
    assertEquals(OrderUiState(), vm.stateFlow.value)
}
```

---

Full runnable example: [`sample/src/test/kotlin/sample/OrderViewModelTest.kt`](https://github.com/fajrbahr/MediatorK/blob/main/sample/src/test/kotlin/sample/OrderViewModelTest.kt)
