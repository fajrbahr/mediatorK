package sample.meditor.orders.queries.getorder

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.Mediator
import com.fajrbahr.mediatork.MediatorRegistrar
import com.fajrbahr.mediatork.RequestContext
import com.fajrbahr.mediatork.handler.RequestHandler
import sample.meditor.orders.queries.query.GetOrderQuery
import sample.meditor.orders.queries.query.OrderDetails

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
