package sample.meditor.orders.delete

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.MediatorRegistrar
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.handler.otherwise
import sample.meditor.orders.validators.DeleteOrderValidator

data class DeleteOrderCommand(
    val orderId: String,
) : Request.Unit

class DeleteOrderFromDbHandler : RequestHandler<DeleteOrderCommand, Unit> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: DeleteOrderCommand,
    ) {
        if (request.orderId.startsWith("ARCHIVED-")) {
            throw IllegalStateException("Order ${request.orderId} not found in active database")
        }
        println("  [DB] Deleted order ${request.orderId} from active database")
    }
}

class DeleteOrderFromArchiveHandler : RequestHandler<DeleteOrderCommand, Unit> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: DeleteOrderCommand,
    ) {
        println("  [ARCHIVE] Deleted order ${request.orderId} from archive storage")
    }
}

val deleteOrderHandler = DeleteOrderFromDbHandler() otherwise DeleteOrderFromArchiveHandler()

class DeleteOrderRegistrar : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.scope {
            +DeleteOrderValidator()
        }
        registry register deleteOrderHandler
    }
}
