package com.fajrbahr.mediatork.test

import com.fajrbahr.mediatork.api.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class GetUserQuery(val id: String) : Request<String>
data class CreateOrderCommand(val id: String) : Request<String>
data class DeleteOrderCommand(val id: String) : Request.Unit
data class StreamItemsQuery(val prefix: String) : StreamRequest<String>
data class OrderPlacedEvent(val orderId: String) : Notification
