package dsl.meditor.orders.create

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestValidator
import com.fajrbahr.mediatork.feature.feature
import com.fajrbahr.mediatork.feature.mapper
import com.fajrbahr.mediatork.feature.validate
import dsl.meditor.context.locale
import kotlin.time.Duration.Companion.seconds

val orderValidatorExtracted: RequestValidator<CreateOrderCommand> = validate<CreateOrderCommand> {
    check(request.amount > 0, "Amount must be positive")
    check(request.amount <= 10_000, "Amount must not exceed 10,000")
    warn(request.amount > 1_000, "High-value order — requires manager approval")
    warn(request.amount > 5_000, "Very high-value order — triggers fraud review")
}

val orderBeforeHookExtracted: suspend (RequestContext, Request<*>) -> Unit =
    { ctx, request ->
        println("Before: Creating order ${request::class.simpleName}")
    }

val orderAfterHookExtracted: suspend (RequestContext, Any?, Request<*>) -> Unit =
    { ctx, result, request ->
        println("After: Order created - $result")
    }

val orderMapperExtracted = mapper<OrderResult, OrderUi> { raw ->
    OrderUi(orderId = raw.orderId)
}

val orderFeatureFullyExtracted = feature<CreateOrderCommand, OrderResult, OrderUi> {
    handle { request ->
        val newOrderId = "ORD-${request.id}"
        println("Creating order $newOrderId with locale ${context.locale}")

        publish(
            OrderCreatedNotification(
                orderId = newOrderId,
                customerEmail = "customer@example.com",
                customerPhone = "+1234567890",
                totalAmount = request.amount,
            )
        )

        OrderResult(orderId = newOrderId, responseTime = 0)
    }


    validate(orderValidatorExtracted)
    before(orderBeforeHookExtracted)
    after(orderAfterHookExtracted)
    mapper(orderMapperExtracted)
}
