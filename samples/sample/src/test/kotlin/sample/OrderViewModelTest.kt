package sample

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.MediatorFactory
import com.fajrbahr.mediatork.api.*
import com.fajrbahr.mediatork.test.fakeHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import sample.android.OrderUiState
import sample.android.OrderViewModel
import sample.orders.commands.createorder.CreateOrderCommand
import sample.orders.commands.createorder.OrderResult
import kotlin.test.*

/**
 * Tests for [OrderViewModel] — no mocking.
 *
 * In a layered architecture the ViewModel sits at the top of a deep dependency chain:
 * ViewModel → UseCase → Repository → DataSource → DB/API. Testing the ViewModel means
 * mocking every layer below it just to compile, even when the test has nothing to do
 * with those layers.
 *
 * MediatorK cuts that chain. The ViewModel has a single dependency — [Mediator] — and
 * each test stubs only the one handler it actually exercises. The rest of the graph is
 * never instantiated.
 *
 * This is the library's core design goal: make the right thing easy (stub what you need),
 * and make the wrong thing hard (you cannot accidentally pull in unrelated dependencies).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OrderViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(testDispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun vmWith(handler: suspend (Mediator, RequestContext, CreateOrderCommand) -> OrderResult): OrderViewModel =
        OrderViewModel(
            MediatorFactory.create(
                registrars = listOf(object : MediatorRegistrar {
                    override fun register(registry: HandlerRegistry) {
                        registry register fakeHandler(handler)
                    }
                })
            )
        )

    // ── happy path ────────────────────────────────────────────────────────────

    @Test
    fun `createOrder success updates stateFlow with result`() = runTest {
        val expected = OrderResult(orderId = "ORD-1", responseIme = 10)
        val vm = vmWith { _, _, _ -> expected }

        vm.createOrder("1", 99.0)
        advanceUntilIdle()

        val state = vm.stateFlow.value
        assertEquals(expected, state.orderResult)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `createOrder success emits success event`() = runTest {
        val vm = vmWith { _, _, _ -> OrderResult(orderId = "ORD-1", responseIme = 5) }

        val events = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.eventFlow.collect { events += it }
        }

        vm.createOrder("1", 99.0)
        advanceUntilIdle()

        assertTrue(events.any { it.contains("successfully", ignoreCase = true) })
    }

    // ── loading state ─────────────────────────────────────────────────────────

    @Test
    fun `isLoading is false after success`() = runTest {
        val vm = vmWith { _, _, _ -> OrderResult(orderId = "ORD-1", responseIme = 1) }

        vm.createOrder("1", 50.0)
        advanceUntilIdle()

        assertFalse(vm.stateFlow.value.isLoading)
    }

    @Test
    fun `isLoading is false after failure`() = runTest {
        val vm = vmWith { _, _, _ -> throw RuntimeException("server down") }

        vm.createOrder("1", 50.0)
        advanceUntilIdle()

        assertFalse(vm.stateFlow.value.isLoading)
    }

    // ── error path ────────────────────────────────────────────────────────────

    @Test
    fun `createOrder failure sets error in stateFlow`() = runTest {
        val vm = vmWith { _, _, _ -> throw RuntimeException("Network unavailable") }

        vm.createOrder("1", 99.0)
        advanceUntilIdle()

        val state = vm.stateFlow.value
        assertEquals("Network unavailable", state.error)
        assertNull(state.orderResult)
        assertFalse(state.isLoading)
    }

    @Test
    fun `createOrder failure emits error event`() = runTest {
        val vm = vmWith { _, _, _ -> throw RuntimeException("timeout") }

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
        var callCount = 0
        val vm = vmWith { _, _, _ ->
            if (callCount++ == 0) throw RuntimeException("first failure")
            else OrderResult(orderId = "ORD-2", responseIme = 2)
        }

        vm.createOrder("1", 10.0)
        advanceUntilIdle()
        assertNotNull(vm.stateFlow.value.error)

        vm.createOrder("2", 20.0)
        advanceUntilIdle()

        assertNull(vm.stateFlow.value.error)
        assertEquals("ORD-2", vm.stateFlow.value.orderResult?.orderId)
    }

    @Test
    fun `initial state is empty and not loading`() {
        val vm = vmWith { _, _, _ -> OrderResult(responseIme = 0) }
        assertEquals(OrderUiState(), vm.stateFlow.value)
    }
}
