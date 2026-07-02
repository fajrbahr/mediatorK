package dsl.meditor.orders.stream

import com.fajrbahr.mediatork.api.StreamRequest

data class OrderUpdatesStream(
    val orderId: String,
) : StreamRequest<OrderUpdate>

data class OrderUpdate(
    val orderId: String,
    val status: String,
)
