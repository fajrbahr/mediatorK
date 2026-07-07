package local.meditor.orders.query

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.MediatorRegistrar
import com.fajrbahr.mediatork.RequestContext
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.handler.RequestHandler

class GetOrderHandler : RequestHandler<GetOrderQuery, OrderDetails> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: GetOrderQuery,
    ): OrderDetails = OrderDetails(
        orderId = request.orderId,
        customerId = request.customerId,
        status = "CONFIRMED",
        totalAmount = 99.99,
    )
}

class GetOrderRegistrar : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.scope {
            +GetOrderHandler()
        }
    }
}
