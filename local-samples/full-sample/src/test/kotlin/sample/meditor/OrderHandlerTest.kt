package sample.meditor

import com.fajrbahr.mediatork.test.*
import com.fajrbahr.mediatork.validator.ValidationException
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import sample.meditor.orders.create.*
import sample.meditor.orders.delete.DeleteOrderCommand
import sample.meditor.orders.delete.DeleteOrderRegistrar
import sample.meditor.orders.queries.getorder.GetOrderRegistrar
import sample.meditor.orders.queries.query.GetOrderQuery
import sample.meditor.orders.queries.query.OrderDetails
import sample.meditor.orders.stream.OrderUpdate
import sample.meditor.orders.stream.OrderUpdatesRegistrar
import sample.meditor.orders.stream.OrderUpdatesStream
import kotlin.test.*

class HandlerTestHarnessTest {

    @Test
    fun `create order returns result with generated ID`() = runTest {
        val harness = buildHandlerTestHarness(
            registrars = listOf(OrderRegistrar(), OrderNotificationRegistrar()),
        )

        val result = harness.send(CreateOrderCommand(id = "42", amount = 50.0))

        assertEquals("ORD-42", result.orderId)
    }

    @Test
    fun `query order after setup with given`() = runTest {
        val harness = buildHandlerTestHarness(
            registrars = listOf(OrderRegistrar(), OrderNotificationRegistrar(), GetOrderRegistrar()),
        )

        harness.given(CreateOrderCommand(id = "1", amount = 10.0))

        val details = harness.query(GetOrderQuery(orderId = "ORD-1", customerId = "USR-1"))
        assertEquals("CONFIRMED", details.status)
    }

    @Test
    fun `stream emits all order statuses`() = runTest {
        val harness = buildHandlerTestHarness(
            registrars = listOf(OrderUpdatesRegistrar()),
        )

        val updates = harness.stream(OrderUpdatesStream(orderId = "ORD-99")).toList()

        assertEquals(4, updates.size)
        assertEquals("RECEIVED", updates.first().status)
        assertEquals("DELIVERED", updates.last().status)
    }

    @Test
    fun `delete order via fallback handler for archived orders`() = runTest {
        val harness = buildHandlerTestHarness(
            registrars = listOf(DeleteOrderRegistrar()),
        )

        // Archived orders fall through to the archive handler — no exception
        harness.send(DeleteOrderCommand(orderId = "ARCHIVED-old"))
    }
}

class FakeMediatorTest {

    @Test
    fun `fake mediator with inline handler`() = runTest {
        val mediator = FakeMediator {
            +fakeHandler<GetOrderQuery, OrderDetails> { _, _, req ->
                OrderDetails(
                    orderId = req.orderId,
                    customerId = req.customerId,
                    status = "MOCK_STATUS",
                    totalAmount = 42.0,
                )
            }
        }

        val result = mediator.send(GetOrderQuery(orderId = "ORD-1", customerId = "USR-1"))
        assertEquals("MOCK_STATUS", result.status)
        assertEquals(42.0, result.totalAmount)
    }

    @Test
    fun `capture notifications collects published events`() = runTest {
        val mediator = FakeMediator(
            registrars = listOf(OrderRegistrar(), OrderNotificationRegistrar()),
        )
        val captured = mediator.captureNotifications<OrderCreatedNotification>()

        mediator.send(CreateOrderCommand(id = "99", amount = 25.0))

        assertEquals(1, captured.size)
        assertEquals("ORD-99", captured.first().orderId)
        assertEquals(25.0, captured.first().totalAmount)
    }

    @Test
    fun `validation rejects invalid order`() = runTest {
        val mediator = FakeMediator(
            registrars = listOf(OrderRegistrar(), OrderNotificationRegistrar()),
        )

        val ex = assertFailsWith<ValidationException> {
            mediator.send(CreateOrderCommand(id = "", amount = 50.0))
        }
        assertTrue(ex.errors.any { it.toString().contains("required") })
    }
}

class MediatorSpyTest {

    @Test
    fun `spy records sent requests and published notifications`() = runTest {
        val fake = FakeMediator(
            registrars = listOf(OrderRegistrar(), OrderNotificationRegistrar()),
        )
        val spy = MediatorSpy(fake)

        spy.send(CreateOrderCommand(id = "1", amount = 50.0))
        spy.send(CreateOrderCommand(id = "2", amount = 75.0))

        spy.assertSent<CreateOrderCommand>()
        spy.assertSentCount<CreateOrderCommand>(2)
        assertEquals("1", spy.sentOf<CreateOrderCommand>().first().id)
    }

    @Test
    fun `spy records streamed requests`() = runTest {
        val fake = FakeMediator(registrars = listOf(OrderUpdatesRegistrar()))
        val spy = MediatorSpy(fake)

        spy.stream(OrderUpdatesStream(orderId = "ORD-1")).toList()

        spy.assertStreamed<OrderUpdatesStream>()
        spy.assertStreamedCount<OrderUpdatesStream>(1)
    }

    @Test
    fun `spy assertNotSent passes when no matching request sent`() = runTest {
        val fake = FakeMediator(registrars = listOf(OrderRegistrar(), OrderNotificationRegistrar()))
        val spy = MediatorSpy(fake)

        spy.send(CreateOrderCommand(id = "1", amount = 10.0))

        spy.assertNotSent<GetOrderQuery>()
        spy.assertNotStreamed<OrderUpdatesStream>()
    }

    @Test
    fun `spy reset clears all recorded state`() = runTest {
        val fake = FakeMediator(registrars = listOf(OrderRegistrar(), OrderNotificationRegistrar()))
        val spy = MediatorSpy(fake)

        spy.send(CreateOrderCommand(id = "1", amount = 10.0))
        spy.assertSent<CreateOrderCommand>()

        spy.reset()

        spy.assertNotSent<CreateOrderCommand>()
        assertTrue(spy.sentRequests.isEmpty())
    }
}

class DummyMediatorTest {

    @Test
    fun `dummy mediator satisfies constructor dependency`() {
        val mediator: com.fajrbahr.mediatork.api.Mediator = DummyMediator()
        assertNotNull(mediator)
    }

    @Test
    fun `dummy mediator publish does nothing`() = runTest {
        val mediator = DummyMediator()
        mediator.publish(OrderCreatedNotification("ORD-1", "a@b.com", "+1", 10.0))
    }

    @Test
    fun `dummy mediator stream returns empty flow`() = runTest {
        val mediator = DummyMediator()

        val items = mediator.stream(OrderUpdatesStream("ORD-1")).toList()
        assertTrue(items.isEmpty())
    }
}
