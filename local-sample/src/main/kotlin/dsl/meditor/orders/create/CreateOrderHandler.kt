package dsl.meditor.orders.create

import com.fajrbahr.mediatork.handler.handler
import com.fajrbahr.mediatork.mediatorRegistrar
import dsl.meditor.context.locale
import dsl.meditor.orders.validators.createOrderValidator

val createOrderHandler = handler<CreateOrderCommand, OrderResult> { request ->
    val newOrderId = "ORD-${request.id}"

    println("Creating order $newOrderId with locale ${requestContext.locale}")

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
