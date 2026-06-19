package sample.orders.queries.getorder

import com.fajrbahr.mediatork.api.RequestValidator
import com.fajrbahr.mediatork.validator.rulesFailFast

class GetOrderQueryValidator : RequestValidator<GetOrderQuery> {
    override fun validate(request: GetOrderQuery) = rulesFailFast<String> {
        check(request.orderId.isNotBlank()) { "Order ID is required" }
        check(request.orderId.startsWith("ORD-")) { "Order ID must start with ORD-" }
        check(request.orderId.length > 4) { "Order ID must have a value after ORD-" }
        check(request.customerId.isNotBlank()) { "Customer ID is required" }
        check(request.customerId.startsWith("USR-")) { "Customer ID must start with USR-" }
    }
}
