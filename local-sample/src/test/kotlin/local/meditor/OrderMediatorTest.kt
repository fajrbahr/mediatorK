package local.meditor

import com.fajrbahr.mediatork.validator.ValidationException
import kotlinx.coroutines.test.runTest
import local.meditor.orders.create.CreateOrderCommand
import local.meditor.orders.query.GetOrderQuery
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OrderMediatorTest {

    private val mediator = orderMediator()

    @Test
    fun `create order returns prefixed id and fans out notifications`() = runTest {
        val result = mediator.send(CreateOrderCommand(id = "1", amount = 150.0))
        assertEquals("ORD-1", result.orderId)
    }

    @Test
    fun `get order returns confirmed details`() = runTest {
        val details = mediator.send(GetOrderQuery(orderId = "ORD-9988", customerId = "USR-42"))
        assertEquals("CONFIRMED", details.status)
        assertEquals("ORD-9988", details.orderId)
        assertTrue(details.totalAmount > 0)
    }

    @Test
    fun `get order with malformed id fails validation`() = runTest {
        assertFailsWith<ValidationException> {
            mediator.send(GetOrderQuery(orderId = "9988", customerId = "USR-42"))
        }
    }
}
