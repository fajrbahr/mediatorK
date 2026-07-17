package local.meditor.orders.create

import com.fajrbahr.mediatork.Handler
import com.fajrbahr.mediatork.api.Request
import local.meditor.context.locale

data class CreateOrderCommand(
    val id: String,
    val amount: Double,
) : Request<OrderResult>

data class OrderResult(
    val orderId: String = "",
    val responseTime: Long,
)
val createOrderHandler: Handler<CreateOrderCommand, OrderResult> = { request ->
    val newOrderId = "ORD-${request.id}"
    println("Creating order $newOrderId with locale ${context.locale}")
    publish(
        OrderCreatedNotification(
            orderId = newOrderId,
            customerEmail = "customer@example.com",
            customerPhone = "+1234567890",
            totalAmount = request.amount,
        )
    )
    OrderResult(orderId = newOrderId, responseTime = 0)
}
