package dsl.meditor.orders.create

import com.fajrbahr.mediatork.api.Notification
import com.fajrbahr.mediatork.api.NotificationHandler
import com.fajrbahr.mediatork.notification.otherwise

data class OrderCreatedNotification(
    val orderId: String,
    val customerEmail: String,
    val customerPhone: String,
    val totalAmount: Double,
) : Notification

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
