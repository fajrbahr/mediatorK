package dsl.meditor.orders.delete

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.handler.handler
import com.fajrbahr.mediatork.handler.otherwise
import com.fajrbahr.mediatork.mediatorRegistrar
import dsl.meditor.orders.validators.deleteOrderValidator

data class DeleteOrderCommand(
    val orderId: String,
) : Request.Unit

val deleteOrderFromDbHandler = handler<DeleteOrderCommand, Unit> { request ->
    if (request.orderId.startsWith("ARCHIVED-")) {
        throw IllegalStateException("Order ${request.orderId} not found in active database")
    }
    println("  [DB] Deleted order ${request.orderId} from active database")
}

val deleteOrderFromArchiveHandler = handler<DeleteOrderCommand, Unit> { request ->
    println("  [ARCHIVE] Deleted order ${request.orderId} from archive storage")
}

val deleteOrderHandler = deleteOrderFromDbHandler otherwise deleteOrderFromArchiveHandler

val deleteOrderRegistrar = mediatorRegistrar {
    register(deleteOrderValidator)
    register(deleteOrderHandler)
}
