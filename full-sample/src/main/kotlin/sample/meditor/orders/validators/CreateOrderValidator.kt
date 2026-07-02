package sample.meditor.orders.validators

import com.fajrbahr.mediatork.api.RequestValidator
import com.fajrbahr.mediatork.validator.ValidationResult
import com.fajrbahr.mediatork.validator.rules
import sample.meditor.orders.create.CreateOrderCommand
import sample.meditor.orders.delete.DeleteOrderCommand

class CreateOrderValidator : RequestValidator<CreateOrderCommand> {
    override fun validate(request: CreateOrderCommand): ValidationResult = rules<String> {
        check(request.id.isNotBlank()) { "Order ID is required" }
        check(request.amount > 0) { "Amount must be positive" }
        check(request.amount <= 10_000) { "Amount must not exceed 10,000" }
        warn(request.amount > 1_000) { "High-value order — requires manager approval" }
        warn(request.amount > 5_000) { "Very high-value order — triggers fraud review" }
    }
}

class DeleteOrderValidator : RequestValidator<DeleteOrderCommand> {
    override fun validate(request: DeleteOrderCommand): ValidationResult = rules<String> {
        check(request.orderId.isNotBlank()) { "Order ID is required" }
    }
}
