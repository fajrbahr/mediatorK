package sample.orders.queries.getorder

import com.fajrbahr.mediatork.*
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.MediatorRegistrar
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler

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
