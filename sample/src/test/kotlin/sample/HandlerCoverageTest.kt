package sample

import com.fajrbahr.mediatork.test.MediatorTestUtils
import sample.command.OrderRegistrar
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
            ),
            packages = listOf("sample"),
        )
    }
}
