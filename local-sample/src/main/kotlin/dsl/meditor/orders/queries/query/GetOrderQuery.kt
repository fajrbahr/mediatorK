package dsl.meditor.orders.queries.query

import com.fajrbahr.mediatork.api.Request


data class OrderDetails(
    val orderId: String,
    val customerId: String,
    val status: String,
    val totalAmount: Double,
)

data class GetOrderQuery(
    val orderId: String,
    val customerId: String,
) : Request<OrderDetails>