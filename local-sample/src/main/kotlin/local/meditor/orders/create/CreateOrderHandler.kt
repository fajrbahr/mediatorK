package local.meditor.orders.create

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.MediatorRegistrar
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.handler.RequestHandler
import local.meditor.context.locale

class CreateOrderHandler : RequestHandler<CreateOrderCommand, OrderResult> {

    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: CreateOrderCommand,
    ): OrderResult {
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

        return OrderResult(orderId = newOrderId, responseTime = 0)
    }
}

class OrderRegistrar : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.scope {
            +CreateOrderHandler()
        }
    }
}
