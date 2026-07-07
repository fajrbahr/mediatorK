package sample.meditor.orders.queries.query

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.validator.rulesFailFast

data class GetOrderQuery(
    val orderId: String,
    val customerId: String,
) : Request<OrderDetails> {
//    fun validate() = rulesFailFast<String> {
//        check(orderId.isNotBlank()) { "Order ID is required" }
//        check(orderId.startsWith("ORD-")) { "Order ID must start with ORD-" }
//        check(orderId.length > 4) { "Order ID must have a value after ORD-" }
//        check(customerId.isNotBlank()) { "Customer ID is required" }
//        check(customerId.startsWith("USR-")) { "Customer ID must start with USR-" }
//    }
}

data class OrderDetails(
    val orderId: String,
    val customerId: String,
    val status: String,
    val totalAmount: Double,
)
