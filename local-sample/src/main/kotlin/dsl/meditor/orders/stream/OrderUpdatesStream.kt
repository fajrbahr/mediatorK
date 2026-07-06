package dsl.meditor.orders.stream

import com.fajrbahr.mediatork.api.StreamRequest
import com.fajrbahr.mediatork.handler.streamHandler
import com.fajrbahr.mediatork.mediatorModule
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

data class OrderUpdatesStream(
    val orderId: String,
) : StreamRequest<OrderUpdate>

data class OrderUpdate(
    val orderId: String,
    val status: String,
)

val orderUpdatesHandler = streamHandler<OrderUpdatesStream, OrderUpdate> { request ->
    flow {
        val statuses = listOf("RECEIVED", "PROCESSING", "SHIPPED", "DELIVERED")
        for (status in statuses) {
            delay(100)
            emit(OrderUpdate(orderId = request.orderId, status = status))
        }
    }
}

val orderUpdatesSlice = mediatorModule {
    add(orderUpdatesHandler)
}
