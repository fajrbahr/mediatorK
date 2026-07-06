package dsl.meditor.orders.delete

import com.fajrbahr.mediatork.api.Notification
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.feature.notificationHandler
import com.fajrbahr.mediatork.handler.handler
import com.fajrbahr.mediatork.handler.orElse
import com.fajrbahr.mediatork.mediatorModule
import com.fajrbahr.mediatork.notification.orElse

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

val deleteOrderHandler = deleteOrderFromDbHandler orElse deleteOrderFromArchiveHandler


data class OrderDeleteNotification(
    val orderId: String,
    val customerEmail: String,
    val customerPhone: String,
    val totalAmount: Double,
) : Notification

val sendOrderSmsHandler = notificationHandler<OrderDeleteNotification> { notification ->
    println("  [SMS] Sent to ${notification.customerPhone} for order ${notification.orderId}")
}

val sendOrderPushHandler = notificationHandler<OrderDeleteNotification> { notification ->
    if (notification.totalAmount > 100) {
        throw RuntimeException("Push service unavailable for high-value orders")
    }
    println("  [PUSH] Push notification sent for order ${notification.orderId}")
}

val deleteOrderNotification = sendOrderPushHandler orElse sendOrderSmsHandler


val deleteOrderSlice = mediatorModule {
    handler(deleteOrderHandler)
    notification(deleteOrderNotification)
}
