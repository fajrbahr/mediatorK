package dsl.meditor.orders.create

import com.fajrbahr.mediatork.api.Notification
import com.fajrbahr.mediatork.feature.notificationHandler

data class OrderCreatedNotification(
    val orderId: String,
    val customerEmail: String,
    val customerPhone: String,
    val totalAmount: Double,
) : Notification


val sendOrderConfirmationEmailHandler = notificationHandler<OrderCreatedNotification> { notification ->
    println("  [EMAIL] Confirmation sent to ${notification.customerEmail} for order ${notification.orderId}")
}
