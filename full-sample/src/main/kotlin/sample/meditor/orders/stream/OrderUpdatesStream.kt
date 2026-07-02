package sample.meditor.orders.stream

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.MediatorRegistrar
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.StreamRequest
import com.fajrbahr.mediatork.api.StreamRequestHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class OrderUpdatesStream(
    val orderId: String,
) : StreamRequest<OrderUpdate>

data class OrderUpdate(
    val orderId: String,
    val status: String,
)

class OrderUpdatesHandler : StreamRequestHandler<OrderUpdatesStream, OrderUpdate> {
    override fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: OrderUpdatesStream,
    ): Flow<OrderUpdate> = flow {
        val statuses = listOf("RECEIVED", "PROCESSING", "SHIPPED", "DELIVERED")
        for (status in statuses) {
            delay(100)
            emit(OrderUpdate(orderId = request.orderId, status = status))
        }
    }
}

class OrderUpdatesRegistrar : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.scope {
            +OrderUpdatesHandler()
        }
    }
}
