package sample.exceptions

import com.fajrbahr.mediatork.AggregateException
import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.MediatorRegistrar
import com.fajrbahr.mediatork.api.Notification
import com.fajrbahr.mediatork.api.NotificationHandler
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.handler.otherwise

// ── Domain ────────────────────────────────────────────────────────────────────

data class ShipOrderCommand(val orderId: String, val warehouseId: String) : Request<ShipOrderResult>

data class ShipOrderResult(val status: String, val orderId: String)

data class ShipOrderShippedNotification(val orderId: String) : Notification

class OrderNotFoundException(orderId: String) : Exception("Order '$orderId' not found")
class OutOfStockException(warehouseId: String) : Exception("Warehouse '$warehouseId' is out of stock")

// ── Handlers ──────────────────────────────────────────────────────────────────

class ShipOrderHandler : RequestHandler<ShipOrderCommand, ShipOrderResult> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: ShipOrderCommand,
    ): ShipOrderResult {
        if (request.orderId == "MISSING") throw OrderNotFoundException(request.orderId)
        if (request.warehouseId == "WH-EMPTY") throw OutOfStockException(request.warehouseId)
        return ShipOrderResult(status = "SHIPPED", orderId = request.orderId)
    }
}

/** Fallback handler — returns a safe default when [ShipOrderHandler] throws. */
class ShipOrderFallbackHandler : RequestHandler<ShipOrderCommand, ShipOrderResult> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: ShipOrderCommand,
    ): ShipOrderResult = ShipOrderResult(status = "FALLBACK_QUEUED", orderId = request.orderId)
}

// ── Notification handlers ─────────────────────────────────────────────────────

class PushShipNotificationHandler(private val fails: Boolean) : NotificationHandler<ShipOrderShippedNotification> {
    override suspend fun handle(notification: ShipOrderShippedNotification) {
        if (fails) throw RuntimeException("Push service is down")
        println("  [Push] Order ${notification.orderId} shipped notification sent")
    }
}

class AnalyticsShipNotificationHandler(private val fails: Boolean) : NotificationHandler<ShipOrderShippedNotification> {
    override suspend fun handle(notification: ShipOrderShippedNotification) {
        if (fails) throw RuntimeException("Analytics service is down")
        println("  [Analytics] Order ${notification.orderId} tracked")
    }
}

// ── Registrar ─────────────────────────────────────────────────────────────────

class ShipOrderRegistrar(
    val pushFails: Boolean = false,
    val analyticsFails: Boolean = false,
) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        // ShipOrderHandler throws; ShipOrderFallbackHandler catches via the `otherwise` chain.
        registry register (ShipOrderHandler() otherwise ShipOrderFallbackHandler())
        registry.registerNotification(PushShipNotificationHandler(pushFails))
        registry.registerNotification(AnalyticsShipNotificationHandler(analyticsFails))
    }
}

// ── Demo ──────────────────────────────────────────────────────────────────────

suspend fun demoContinueOnException(mediator: Mediator) {
    try {
        mediator.publish(ShipOrderShippedNotification(orderId = "ORD-DEMO"))
    } catch (e: AggregateException) {
        println("  AggregateException caught: ${e.message}")
    }
}
