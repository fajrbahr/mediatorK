package sample

import com.fajrbahr.mediatork.test.MediatorTestUtils
import sample.bookings.queries.fetchbookings.FetchBookingsRegistrar
import sample.exceptions.ShipOrderRegistrar
import sample.fallback.FallbackRegistrar
import sample.invoice.InvoiceRegistrar
import sample.invoice.InvoiceRepository
import sample.orders.commands.createorder.OrderNotificationRegistrar
import sample.orders.commands.createorder.OrderRegistrar
import sample.orders.queries.getorder.GetOrderRegistrar
import sample.users.queries.fetchuser.UserRegistrar
import kotlin.test.Test

class HandlerCoverageTest {

    @Test
    fun `all handlers are registered`() {
        MediatorTestUtils.assertAllHandlersRegistered(
            registrars = listOf(
                UserRegistrar(),
                OrderRegistrar(),
                OrderNotificationRegistrar(),
                FetchBookingsRegistrar(),
                GetOrderRegistrar(),
                ShipOrderRegistrar(),
                FallbackRegistrar(),
                InvoiceRegistrar(InvoiceRepository()),
            ),
            packages = listOf("sample"),
        )
    }
}
