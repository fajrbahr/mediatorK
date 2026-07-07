package local.meditor.orders.query

import com.fajrbahr.mediatork.api.Request

data class GetOrderQuery(
    val orderId: String,
    val customerId: String,
) : Request<OrderDetails>

data class OrderDetails(
    val orderId: String,
    val customerId: String,
    val status: String,
    val totalAmount: Double,
)
