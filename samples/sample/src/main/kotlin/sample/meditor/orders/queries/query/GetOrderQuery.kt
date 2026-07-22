package sample.meditor.orders.queries.query

import com.fajrbahr.mediatork.Handler
import com.fajrbahr.mediatork.Validator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.validator.rules

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

val getOrderHandler: Handler<GetOrderQuery, OrderDetails> = { request ->
    OrderDetails(
        orderId = request.orderId,
        customerId = request.customerId,
        status = "CONFIRMED",
        totalAmount = 99.99,
    )
}

val getOrderValidator: Validator<GetOrderQuery> = { query ->
    rules {
        check(query.orderId.startsWith("ORD-")) { "Order ID must start with ORD-" }
        check(query.customerId.startsWith("USR-")) { "Customer ID must start with USR-" }
    }
}
