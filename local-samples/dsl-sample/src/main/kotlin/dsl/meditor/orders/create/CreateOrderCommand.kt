package dsl.meditor.orders.create

import com.fajrbahr.mediatork.api.Request

data class CreateOrderCommand(
    val id: String,
    val amount: Double,
) : Request<OrderResult>

data class OrderResult(
    val orderId: String = "",
    val responseTime: Long,
)
