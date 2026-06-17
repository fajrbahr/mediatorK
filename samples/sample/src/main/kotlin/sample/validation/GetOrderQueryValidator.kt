package sample.validation

import com.fajrbahr.mediatork.validator.FieldValidator
import com.fajrbahr.mediatork.validator.RequestValidator
import com.fajrbahr.mediatork.validator.ValidationResult
import com.fajrbahr.mediatork.validator.rulesFailFast
import sample.query.GetOrderQuery
import kotlin.reflect.KClass

class GetOrderQueryValidator : RequestValidator<GetOrderQuery> {
    override val requestClass: KClass<GetOrderQuery> = GetOrderQuery::class

    override fun validate(request: GetOrderQuery): ValidationResult = rulesFailFast {
        ruleFor(GetOrderField.OrderId, request.orderId) {
            check(it.isNotBlank()) { "Order ID is required" }
            check(it.startsWith("ORD-")) { "Order ID must start with ORD-" }
            check(it.length > 4) { "Order ID must have a value after ORD-" }
        }
        ruleFor(GetOrderField.CustomerId, request.customerId) {
            check(it.isNotBlank()) { "Customer ID is required" }
            check(it.startsWith("USR-")) { "Customer ID must start with USR-" }
        }
    }
}

sealed class GetOrderField : FieldValidator {
    object OrderId : GetOrderField()
    object CustomerId : GetOrderField()
}
