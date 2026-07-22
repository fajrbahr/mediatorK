package sample.meditor.orders.create

import com.fajrbahr.mediatork.NotificationHandler
import com.fajrbahr.mediatork.api.Notification

data class OrderCreatedNotification(
    val orderId: String,
    val customerEmail: String,
    val customerPhone: String,
    val totalAmount: Double,
) : Notification

val sendOrderConfirmationEmailHandler: NotificationHandler<OrderCreatedNotification> = {
    println("email: Order ${it.orderId} confirmation sent to ${it.customerEmail}")
}

val sendOrderSmsHandler: NotificationHandler<OrderCreatedNotification> = {
    println("sms: SMS sent to ${it.customerPhone} for order ${it.orderId}")
}
