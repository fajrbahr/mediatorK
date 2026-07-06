package dsl.meditor.orders.create

import com.fajrbahr.mediatork.api.Request

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
