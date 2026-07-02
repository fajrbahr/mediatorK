package sample.meditor.orders.create

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.MediatorRegistrar
import com.fajrbahr.mediatork.api.Notification
import com.fajrbahr.mediatork.api.NotificationHandler
import com.fajrbahr.mediatork.notification.otherwise

data class OrderCreatedNotification(
    val orderId: String,
    val customerEmail: String,
    val customerPhone: String,
    val totalAmount: Double,
) : Notification

class SendOrderConfirmationEmailHandler : NotificationHandler<OrderCreatedNotification> {
    override suspend fun handle(notification: OrderCreatedNotification) {
        println("  [EMAIL] Confirmation sent to ${notification.customerEmail} for order ${notification.orderId}")
    }
}

class SendOrderSmsHandler : NotificationHandler<OrderCreatedNotification> {
    override suspend fun handle(notification: OrderCreatedNotification) {
        println("  [SMS] Sent to ${notification.customerPhone} for order ${notification.orderId}")
    }
}

class AuditLogNotificationHandler : NotificationHandler<OrderCreatedNotification> {
    override suspend fun handle(notification: OrderCreatedNotification) {
        println("  [AUDIT] Order ${notification.orderId} logged, amount: ${notification.totalAmount}")
    }
}

class SendOrderPushHandler : NotificationHandler<OrderCreatedNotification> {
    override suspend fun handle(notification: OrderCreatedNotification) {
        if (notification.totalAmount > 100) {
            throw RuntimeException("Push service unavailable for high-value orders")
        }
        println("  [PUSH] Push notification sent for order ${notification.orderId}")
    }
}

class SendOrderInAppHandler : NotificationHandler<OrderCreatedNotification> {
    override suspend fun handle(notification: OrderCreatedNotification) {
        println("  [IN-APP] In-app notification sent for order ${notification.orderId}")
    }
}

val pushWithFallback = SendOrderPushHandler() otherwise SendOrderInAppHandler()

data class UnhandledNotification(val info: String = "test") : Notification

class OrderNotificationRegistrar : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.scope {
            +SendOrderConfirmationEmailHandler()
            +SendOrderSmsHandler()
            +AuditLogNotificationHandler()
        }
        registry registerNotification pushWithFallback
    }
}
