package sample.query
import com.fajrbahr.mediatork.handler.*

import com.fajrbahr.mediatork.*

data class GetOrderQuery(
    val orderId: String,
    val customerId: String,
) : Request<OrderDetails>

data class OrderDetails(
    val orderId: String,
    val customerId: String,
    val status: String,
    val totalAmount: Double,
)

class GetOrderHandler : RequestHandler<GetOrderQuery, OrderDetails> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: GetOrderQuery,
    ): OrderDetails {
        return OrderDetails(
            orderId = request.orderId,
            customerId = request.customerId,
            status = "CONFIRMED",
            totalAmount = 99.99,
        )
    }
}

class GetOrderRegistrar : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.scope {
            +GetOrderHandler()
        }
    }
}
