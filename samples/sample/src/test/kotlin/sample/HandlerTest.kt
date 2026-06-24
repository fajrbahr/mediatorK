package sample

import com.fajrbahr.mediatork.MediatorFactory
import com.fajrbahr.mediatork.validator.ValidationException
import kotlinx.coroutines.test.runTest
import sample.bookings.queries.fetchbookings.FetchBookingsQuery
import sample.bookings.queries.fetchbookings.FetchBookingsRegistrar
import sample.exceptions.ShipOrderCommand
import sample.exceptions.ShipOrderRegistrar
import sample.fallback.FallbackRegistrar
import sample.orders.commands.createorder.CreateOrderCommand
import kotlin.test.*

/** Integration tests for the remaining handlers — no mocking. */
class HandlerTest {

    // ── FetchBookings ─────────────────────────────────────────────────────────

    @Test
    fun `fetch bookings returns booking for valid input`() = runTest {
        val mediator = MediatorFactory.create(
            registrars = listOf(FetchBookingsRegistrar()),
            verifyHandlers = false,
        )
        val booking = mediator.send(
            FetchBookingsQuery(userEmail = "user@example.com", bookingId = "bx_booking#1"),
        )
        assertEquals("bx_booking#1", booking.bookingId)
        assertEquals("user@example.com", booking.userEmail)
    }

    @Test
    fun `fetch bookings validation rejects invalid email`() = runTest {
        val mediator = MediatorFactory.create(
            registrars = listOf(FetchBookingsRegistrar()),
            verifyHandlers = false,
        )
        assertFailsWith<ValidationException> {
            mediator.send(FetchBookingsQuery(userEmail = "notanemail", bookingId = "bx_booking#1"))
        }
    }

    @Test
    fun `fetch bookings validation rejects wrong booking ID prefix`() = runTest {
        val mediator = MediatorFactory.create(
            registrars = listOf(FetchBookingsRegistrar()),
            verifyHandlers = false,
        )
        assertFailsWith<ValidationException> {
            mediator.send(FetchBookingsQuery(userEmail = "user@example.com", bookingId = "BOOKING-123"))
        }
    }

    // ── ShipOrder exception handling ──────────────────────────────────────────

    @Test
    fun `ship order falls back when order is missing`() = runTest {
        val mediator = MediatorFactory.create(
            registrars = listOf(ShipOrderRegistrar()),
            verifyHandlers = false,
        )
        val result = mediator.send(ShipOrderCommand(orderId = "MISSING", warehouseId = "WH-1"))
        assertEquals("FALLBACK_QUEUED", result.status)
    }

    @Test
    fun `ship order falls back when warehouse is out of stock`() = runTest {
        val mediator = MediatorFactory.create(
            registrars = listOf(ShipOrderRegistrar()),
            verifyHandlers = false,
        )
        val result = mediator.send(ShipOrderCommand(orderId = "ORD-1", warehouseId = "WH-EMPTY"))
        assertEquals("FALLBACK_QUEUED", result.status)
    }

    @Test
    fun `ship order succeeds for valid input`() = runTest {
        val mediator = MediatorFactory.create(
            registrars = listOf(ShipOrderRegistrar()),
            verifyHandlers = false,
        )
        val result = mediator.send(ShipOrderCommand(orderId = "ORD-42", warehouseId = "WH-1"))
        assertEquals("SHIPPED", result.status)
        assertEquals("ORD-42", result.orderId)
    }

    // ── Request fallback ──────────────────────────────────────────────────────

    @Test
    fun `fallback registrar serves from cache when live API is down`() = runTest {
        val mediator = MediatorFactory.create(
            registrars = listOf(FallbackRegistrar()),
            verifyHandlers = false,
        )
        val result = mediator.send(CreateOrderCommand(id = "FB-1", amount = 50.0))
        assertEquals("FB-1", result.orderId)
    }
}
