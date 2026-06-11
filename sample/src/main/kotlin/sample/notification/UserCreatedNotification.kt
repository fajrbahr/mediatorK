package sample.notification

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.MediatorRegistrar
import com.fajrbahr.mediatork.notification.Notification
import com.fajrbahr.mediatork.notification.NotificationHandler

data class OrderCreatedNotification(
    val orderId: String,
    val customerEmail: String,
    val customerPhone: String,
    val totalAmount: Double
) : Notification

class SendOrderConfirmationEmailHandler :
    NotificationHandler<OrderCreatedNotification> {
    override suspend fun handle(notification: OrderCreatedNotification) {
        println("📧 SendOrderConfirmationEmailHandler: Order ${notification.orderId} confirmation sent to ${notification.customerEmail}")
        // real implementation: emailService.sendOrderConfirmation(notification)
    }
}

class SendOrderSmsHandler :
    NotificationHandler<OrderCreatedNotification> {
    override suspend fun handle(notification: OrderCreatedNotification) {
        println("📱 SendOrderSmsHandler: SMS sent to ${notification.customerPhone} for order ${notification.orderId}")
        // smsService.send(notification.customerPhone, "Your order ${notification.orderId} has been received")
    }
}

class TrackOrderAnalyticsHandler :
    NotificationHandler<OrderCreatedNotification> {
    override suspend fun handle(notification: OrderCreatedNotification) {
        println("📊 TrackOrderAnalyticsHandler: Order ${notification.orderId} ($${notification.totalAmount}) tracked")
        // analytics.track("order_created", mapOf("orderId" to notification.orderId, "amount" to notification.totalAmount))
    }
}

class UpdateInventoryHandler :
    NotificationHandler<OrderCreatedNotification> {
    override suspend fun handle(notification: OrderCreatedNotification) {
        println("📦 UpdateInventoryHandler: Inventory reserved for order ${notification.orderId}")
        // inventoryService.reserveItems(notification.orderId)
    }
}

// ---------- Registrar ----------
class OrderNotificationRegistrar : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.scope {
            +SendOrderConfirmationEmailHandler()
            +SendOrderSmsHandler()
            +TrackOrderAnalyticsHandler()
            registerNotification(UpdateInventoryHandler())
        }
    }
}