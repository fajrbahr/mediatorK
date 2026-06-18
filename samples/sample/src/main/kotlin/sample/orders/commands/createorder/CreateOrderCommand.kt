package sample.orders.commands.createorder

import com.fajrbahr.mediatork.Request

data class CreateOrderCommand(
    val id: String,
    val amount: Double,
) : Request<OrderResult>

data class OrderResult(
    val orderId: String = "",
    val cart: List<String> = emptyList(),
    val responseIme: Long,
)
