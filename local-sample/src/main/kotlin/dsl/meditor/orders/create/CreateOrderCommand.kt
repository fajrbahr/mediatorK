package dsl.meditor.orders.create

import com.fajrbahr.mediatork.api.Notification
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.feature.notificationHandler

data class CreateOrderCommand(
    val id: String,
    val amount: Double,
) : Request<OrderUi>

data class OrderResult(
    val orderId: String = "",
    val responseTime: Long,
)

data class OrderUi(
    val orderId: String = "",
)

data class GetOrderQuery(
    val orderId: String,
) : Request<OrderInfo>

data class OrderInfo(
    val orderId: String,
    val amount: Double,
)

data class OrderCreatedNotification(
    val orderId: String,
    val customerEmail: String,
    val customerPhone: String,
    val totalAmount: Double,
) : Notification


val sendOrderConfirmationEmailHandler = notificationHandler<OrderCreatedNotification> { notification ->
    println("  [EMAIL] Confirmation sent to ${notification.customerEmail} for order ${notification.orderId}")
}
