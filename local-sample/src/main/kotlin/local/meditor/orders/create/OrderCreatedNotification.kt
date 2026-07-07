package local.meditor.orders.create

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.MediatorRegistrar
import com.fajrbahr.mediatork.api.Notification
import com.fajrbahr.mediatork.api.NotificationHandler

data class OrderCreatedNotification(
    val orderId: String,
    val customerEmail: String,
    val customerPhone: String,
    val totalAmount: Double,
) : Notification

class SendOrderConfirmationEmailHandler : NotificationHandler<OrderCreatedNotification> {
    override suspend fun handle(notification: OrderCreatedNotification) {
        println("SendOrderConfirmationEmailHandler: Order ${notification.orderId} confirmation sent to ${notification.customerEmail}")
    }
}

class SendOrderSmsHandler : NotificationHandler<OrderCreatedNotification> {
    override suspend fun handle(notification: OrderCreatedNotification) {
        println("SendOrderSmsHandler: SMS sent to ${notification.customerPhone} for order ${notification.orderId}")
    }
}

class OrderNotificationRegistrar : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry registerNotification SendOrderConfirmationEmailHandler()
        registry registerNotification SendOrderSmsHandler()
    }
}
