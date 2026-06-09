package sample

import com.fajrbahr.mediatork.*
import com.fajrbahr.mediatork.test.FakeMediator
import com.fajrbahr.mediatork.test.fakeHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import sample.android.OrderUiState
import sample.android.OrderViewModel
import sample.command.CreateOrderCommand
import sample.command.OrderResult
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class OrderViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeMediator: FakeMediator
    private lateinit var vm: OrderViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeMediator = FakeMediator()
        vm = OrderViewModel(fakeMediator)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── happy path ────────────────────────────────────────────────────────────

    @Test
    fun `createOrder success updates stateFlow with result`() = runTest {
        val expected = OrderResult(orderId = "ORD-1", responseIme = 10)
        fakeMediator.register(fakeHandler<CreateOrderCommand, OrderResult> { _, _, _ -> expected })

        vm.createOrder("1", 99.0)
        advanceUntilIdle()

        val state = vm.stateFlow.value
        assertEquals(expected, state.orderResult)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `createOrder success emits success event`() = runTest {
        fakeMediator.register(fakeHandler<CreateOrderCommand, OrderResult> { _, _, _ ->
            OrderResult(orderId = "ORD-1", responseIme = 5)
        })

        val events = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.eventFlow.collect { events += it }
        }

        vm.createOrder("1", 99.0)
        advanceUntilIdle()

        assertTrue(events.any { it.contains("successfully", ignoreCase = true) })
    }

    // ── fakeHandler usage ─────────────────────────────────────────────────────

    @Test
    fun `fakeHandler can be registered and used`() = runTest {
        val expected = OrderResult(orderId = "ORD-1", responseIme = 10)

        val handler = fakeHandler<CreateOrderCommand, OrderResult> { _, _, _ -> expected }
        fakeMediator.register(handler)

        vm.createOrder("1", 99.0)
        advanceUntilIdle()

        assertEquals(expected, vm.stateFlow.value.orderResult)
    }

    // ── loading state ─────────────────────────────────────────────────────────

    @Test
    fun `isLoading is false after success`() = runTest {
        fakeMediator.register(fakeHandler<CreateOrderCommand, OrderResult> { _, _, _ ->
            OrderResult(orderId = "ORD-1", responseIme = 1)
        })

        vm.createOrder("1", 50.0)
        advanceUntilIdle()

        assertFalse(vm.stateFlow.value.isLoading)
    }

    @Test
    fun `isLoading is false after failure`() = runTest {
        fakeMediator.register(fakeHandler<CreateOrderCommand, OrderResult> { _, _, _ ->
            throw RuntimeException("server down")
        })

        vm.createOrder("1", 50.0)
        advanceUntilIdle()

        assertFalse(vm.stateFlow.value.isLoading)
    }

    // ── error path ────────────────────────────────────────────────────────────

    @Test
    fun `createOrder failure sets error in stateFlow`() = runTest {
        fakeMediator.register(fakeHandler<CreateOrderCommand, OrderResult> { _, _, _ ->
            throw RuntimeException("Network unavailable")
        })

        vm.createOrder("1", 99.0)
        advanceUntilIdle()

        val state = vm.stateFlow.value
        assertEquals("Network unavailable", state.error)
        assertNull(state.orderResult)
        assertFalse(state.isLoading)
    }

    @Test
    fun `createOrder failure emits error event`() = runTest {
        fakeMediator.register(fakeHandler<CreateOrderCommand, OrderResult> { _, _, _ ->
            throw RuntimeException("timeout")
        })

        val events = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.eventFlow.collect { events += it }
        }

        vm.createOrder("1", 99.0)
        advanceUntilIdle()

        assertTrue(events.any { it.contains("Error", ignoreCase = true) })
    }

    // ── state reset ───────────────────────────────────────────────────────────

    @Test
    fun `error is cleared on the next createOrder call`() = runTest {
        fakeMediator.register(fakeHandler<CreateOrderCommand, OrderResult> { _, _, _ ->
            throw RuntimeException("first failure")
        })
        vm.createOrder("1", 10.0)
        advanceUntilIdle()
        assertNotNull(vm.stateFlow.value.error)

        fakeMediator.register(fakeHandler<CreateOrderCommand, OrderResult> { _, _, _ ->
            OrderResult(orderId = "ORD-2", responseIme = 2)
        })
        vm.createOrder("2", 20.0)
        advanceUntilIdle()

        assertNull(vm.stateFlow.value.error)
        assertEquals("ORD-2", vm.stateFlow.value.orderResult?.orderId)
    }

    @Test
    fun `initial state is empty and not loading`() {
        assertEquals(OrderUiState(), vm.stateFlow.value)
    }
}
