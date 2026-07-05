package dsl.meditor.orders.validators

import com.fajrbahr.mediatork.api.RequestValidator
import com.fajrbahr.mediatork.feature.requestValidator
import com.fajrbahr.mediatork.validator.rules
import dsl.meditor.orders.create.CreateOrderCommand
import dsl.meditor.orders.delete.DeleteOrderCommand

val createOrderValidator: RequestValidator<CreateOrderCommand> = requestValidator { request ->
    rules<String> {
        require(request.id.isNotBlank()) { "Order ID is required" }
        check(request.amount > 0) { "Amount must be positive" }
        check(request.amount <= 10_000) { "Amount must not exceed 10,000" }
        warn(request.amount > 1_000) { "High-value order — requires manager approval" }
        warn(request.amount > 5_000) { "Very high-value order — triggers fraud review" }
    }
}

val deleteOrderValidator: RequestValidator<DeleteOrderCommand> = requestValidator { request ->
    rules<String> {
        check(request.orderId.isNotBlank()) { "Order ID is required" }
    }
}
