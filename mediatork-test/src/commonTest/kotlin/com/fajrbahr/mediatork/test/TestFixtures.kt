package com.fajrbahr.mediatork.test

import com.fajrbahr.mediatork.api.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class GetUserQuery(val id: String) : Request<String>
data class CreateOrderCommand(val id: String) : Request<String>
data class DeleteOrderCommand(val id: String) : Request.Unit
data class StreamItemsQuery(val prefix: String) : StreamRequest<String>
data class OrderPlacedEvent(val orderId: String) : Notification
data class UserDeletedEvent(val userId: String) : Notification

class GetUserHandler : RequestHandler<GetUserQuery, String> {
    override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: GetUserQuery) =
        "user:${request.id}"
}

class CreateOrderHandler : RequestHandler<CreateOrderCommand, String> {
    override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: CreateOrderCommand) =
        "order:${request.id}"
}

class DeleteOrderHandler : RequestHandler<DeleteOrderCommand, Unit> {
    var lastId: String? = null
    override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: DeleteOrderCommand) {
        lastId = request.id
    }
}

class StreamItemsHandler : StreamRequestHandler<StreamItemsQuery, String> {
    override fun handle(mediator: Mediator, requestContext: RequestContext, request: StreamItemsQuery): Flow<String> =
        flow {
            emit("${request.prefix}-1")
            emit("${request.prefix}-2")
            emit("${request.prefix}-3")
        }
}

class OrderPlacedHandler : NotificationHandler<OrderPlacedEvent> {
    val received = mutableListOf<String>()
    override suspend fun handle(notification: OrderPlacedEvent) {
        received += notification.orderId
    }
}
