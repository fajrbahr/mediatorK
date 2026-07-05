package dsl.meditor.orders.create

import com.fajrbahr.mediatork.handler.handler
import com.fajrbahr.mediatork.mediatorRegistrar
import dsl.meditor.context.locale
import dsl.meditor.orders.validators.createOrderValidator

val createOrderHandler = handler<CreateOrderCommand, OrderResult> { request ->
    val newOrderId = "ORD-${request.id}"
    val warnings = requestContext.getMetaData<List<*>>("validation_warnings")

    println("Creating order $newOrderId with locale ${requestContext.locale}")
    if (!warnings.isNullOrEmpty()) {
        println("  [WARN] ${warnings.joinToString("; ")}")
    }

    mediator.publish(
        OrderCreatedNotification(
            orderId = newOrderId,
            customerEmail = "customer@example.com",
            customerPhone = "+1234567890",
            totalAmount = request.amount,
        )
    )

    OrderResult(orderId = newOrderId, responseTime = 0)
}

val orderRegistrar = mediatorRegistrar {
    register(createOrderHandler)
    register(createOrderValidator)
}
