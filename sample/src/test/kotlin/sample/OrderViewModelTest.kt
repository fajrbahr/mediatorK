package sample

import com.fajrbahr.mediatork.Mediator
import com.fajrbahr.mediatork.Notification
import com.fajrbahr.mediatork.NotificationPublisher
import com.fajrbahr.mediatork.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import sample.android.OrderUiState
import sample.android.OrderViewModel
import sample.command.OrderResult
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class OrderViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // Fake mediator — control what send() returns per test
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
        fakeMediator.result = expected

        vm.createOrder("1", 99.0)
        advanceUntilIdle()

        val state = vm.stateFlow.value
        assertEquals(expected, state.orderResult)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `createOrder success emits success event`() = runTest {
        fakeMediator.result = OrderResult(orderId = "ORD-1", responseIme = 5)

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
        fakeMediator.result = OrderResult(orderId = "ORD-1", responseIme = 1)

        vm.createOrder("1", 50.0)
        advanceUntilIdle()

        assertFalse(vm.stateFlow.value.isLoading)
    }

    @Test
    fun `isLoading is false after failure`() = runTest {
        fakeMediator.result = RuntimeException("server down")

        vm.createOrder("1", 50.0)
        advanceUntilIdle()

        assertFalse(vm.stateFlow.value.isLoading)
    }

    // ── error path ────────────────────────────────────────────────────────────

    @Test
    fun `createOrder failure sets error in stateFlow`() = runTest {
        fakeMediator.result = RuntimeException("Network unavailable")

        vm.createOrder("1", 99.0)
        advanceUntilIdle()

        val state = vm.stateFlow.value
        assertEquals("Network unavailable", state.error)
        assertNull(state.orderResult)
        assertFalse(state.isLoading)
    }

    @Test
    fun `createOrder failure emits error event`() = runTest {
        fakeMediator.result = RuntimeException("timeout")

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
        fakeMediator.result = RuntimeException("first failure")
        vm.createOrder("1", 10.0)
        advanceUntilIdle()
        assertNotNull(vm.stateFlow.value.error)

        fakeMediator.result = OrderResult(orderId = "ORD-2", responseIme = 2)
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

// ─── Fake Mediator ────────────────────────────────────────────────────────────

private class FakeMediator : Mediator {
    var result: Any? = OrderResult(orderId = "ORD-default", responseIme = 0)

    @Suppress("UNCHECKED_CAST")
    override suspend fun <TReq : Request<TRes>, TRes> send(request: TReq): TRes {
        val r = result
        if (r is Throwable) throw r
        return r as TRes
    }

    override suspend fun <T : Notification> publish(notification: T) = Unit
    override suspend fun <T : Notification> publish(notification: T, publisher: NotificationPublisher) = Unit
}
