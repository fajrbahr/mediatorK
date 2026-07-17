package local.meditor.orders.query

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
    rules { check(query.orderId.startsWith("ORD-")) { "orderId must start with 'ORD-'" } }
}
