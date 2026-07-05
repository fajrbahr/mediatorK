package dsl.meditor.orders.create

import com.fajrbahr.mediatork.api.Notification
import com.fajrbahr.mediatork.feature.notificationHandler
import com.fajrbahr.mediatork.mediatorRegistrar
import com.fajrbahr.mediatork.notification.otherwise

data class OrderCreatedNotification(
    val orderId: String,
    val customerEmail: String,
    val customerPhone: String,
    val totalAmount: Double,
) : Notification

val sendOrderConfirmationEmailHandler = notificationHandler<OrderCreatedNotification> { notification ->
    println("  [EMAIL] Confirmation sent to ${notification.customerEmail} for order ${notification.orderId}")
}

val sendOrderSmsHandler = notificationHandler<OrderCreatedNotification> { notification ->
    println("  [SMS] Sent to ${notification.customerPhone} for order ${notification.orderId}")
}

val auditLogNotificationHandler = notificationHandler<OrderCreatedNotification> { notification ->
    println("  [AUDIT] Order ${notification.orderId} logged, amount: ${notification.totalAmount}")
}

val sendOrderPushHandler = notificationHandler<OrderCreatedNotification> { notification ->
    if (notification.totalAmount > 100) {
        throw RuntimeException("Push service unavailable for high-value orders")
    }
    println("  [PUSH] Push notification sent for order ${notification.orderId}")
}

val sendOrderInAppHandler = notificationHandler<OrderCreatedNotification> { notification ->
    println("  [IN-APP] In-app notification sent for order ${notification.orderId}")
}

val pushWithFallback = sendOrderPushHandler otherwise sendOrderInAppHandler

data class UnhandledNotification(val info: String = "test") : Notification

val orderNotificationRegistrar = mediatorRegistrar {
    register(sendOrderConfirmationEmailHandler)
    register(sendOrderSmsHandler)
    register(auditLogNotificationHandler)
    register(pushWithFallback)
}
