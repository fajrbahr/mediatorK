package sample

import com.fajrbahr.mediatork.test.MediatorTestUtils
import sample.command.OrderRegistrar
import sample.exceptions.ShipOrderRegistrar
import sample.fallback.FallbackRegistrar
import sample.invoice.InvoiceRegistrar
import sample.invoice.InvoiceRepository
import sample.notification.OrderNotificationRegistrar
import sample.query.FetchUserHandlerRegistrar
import sample.query.GetOrderRegistrar
import sample.query.UserRegistrar
import kotlin.test.Test

class HandlerCoverageTest {

    @Test
    fun `all handlers are registered`() {
        MediatorTestUtils.assertAllHandlersRegistered(
            registrars = listOf(
                UserRegistrar(),
                OrderRegistrar(),
                OrderNotificationRegistrar(),
                FetchUserHandlerRegistrar(),
                GetOrderRegistrar(),
                ShipOrderRegistrar(),
                FallbackRegistrar(),
                InvoiceRegistrar(InvoiceRepository()),
            ),
            packages = listOf("sample"),
        )
    }
}
