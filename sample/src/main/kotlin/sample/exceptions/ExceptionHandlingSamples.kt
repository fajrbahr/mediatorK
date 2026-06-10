package sample.exceptions
import com.fajrbahr.mediatork.handler.*
import com.fajrbahr.mediatork.notification.*

import com.fajrbahr.mediatork.AggregateException
import com.fajrbahr.mediatork.ContinueOnExceptionNotificationPublisher
import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.Mediator
import com.fajrbahr.mediatork.MediatorRegistrar
import com.fajrbahr.mediatork.Notification
import com.fajrbahr.mediatork.NotificationHandler
import com.fajrbahr.mediatork.Request
import com.fajrbahr.mediatork.RequestContext
import com.fajrbahr.mediatork.RequestExceptionHandler
import com.fajrbahr.mediatork.RequestHandler

// ── Domain types ──────────────────────────────────────────────────────────────

data class ShipOrderCommand(val orderId: String, val warehouseId: String) : Request<ShipmentResult>

data class ShipmentResult(val trackingNumber: String, val status: String)

/** Thrown by the handler when the requested order does not exist. */
class OrderNotFoundException(val orderId: String) :
    Exception("Order '$orderId' not found")

/** Thrown when the target warehouse has no stock available. */
class OutOfStockException(val warehouseId: String) :
    Exception("Warehouse '$warehouseId' is out of stock")

data class OrderShippedNotification(
    val orderId: String,
    val trackingNumber: String,
) : Notification

// ── Handler that throws domain exceptions ─────────────────────────────────────

class ShipOrderHandler : RequestHandler<ShipOrderCommand, ShipmentResult> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: ShipOrderCommand,
    ): ShipmentResult {
        if (request.orderId == "MISSING") throw OrderNotFoundException(request.orderId)
        if (request.warehouseId == "WH-EMPTY") throw OutOfStockException(request.warehouseId)

        val tracking = "TRK-${request.orderId}-${request.warehouseId}"
        mediator.publish(OrderShippedNotification(request.orderId, tracking))
        return ShipmentResult(trackingNumber = tracking, status = "SHIPPED")
    }
}

// ── Exception handlers ────────────────────────────────────────────────────────

/**
 * Converts [OrderNotFoundException] into a graceful "not found" result instead
 * of letting the exception propagate to the caller.
 *
 * Register via [HandlerRegistry.registerExceptionHandler].
 */
class OrderNotFoundExceptionHandler :
    RequestExceptionHandler<ShipOrderCommand, ShipmentResult, OrderNotFoundException> {
    override suspend fun handle(
        requestContext: RequestContext,
        request: ShipOrderCommand,
        exception: OrderNotFoundException,
    ): ShipmentResult {
        println("⚠️  OrderNotFoundExceptionHandler: ${exception.message} — returning CANCELLED result")
        return ShipmentResult(trackingNumber = "N/A", status = "CANCELLED — order not found")
    }
}

/**
 * Converts [OutOfStockException] into a "pending" result so the caller can retry
 * once stock is replenished, rather than crashing with an unhandled exception.
 */
class OutOfStockExceptionHandler :
    RequestExceptionHandler<ShipOrderCommand, ShipmentResult, OutOfStockException> {
    override suspend fun handle(
        requestContext: RequestContext,
        request: ShipOrderCommand,
        exception: OutOfStockException,
    ): ShipmentResult {
        println("⚠️  OutOfStockExceptionHandler: ${exception.message} — returning PENDING result")
        return ShipmentResult(trackingNumber = "N/A", status = "PENDING — awaiting restock at ${exception.warehouseId}")
    }
}

// ── Notification handlers (some intentionally fail) ───────────────────────────

class EmailShipmentHandler : NotificationHandler<OrderShippedNotification> {
    override suspend fun handle(notification: OrderShippedNotification) {
        println("📧 EmailShipmentHandler: confirmation sent for ${notification.orderId}")
    }
}

class SmsShipmentHandler : NotificationHandler<OrderShippedNotification> {
    override suspend fun handle(notification: OrderShippedNotification) {
        println("📱 SmsShipmentHandler: SMS sent for ${notification.orderId}")
    }
}

/** Simulates a flaky downstream system that sometimes fails. */
class PushShipmentHandler(private val shouldFail: Boolean = false) :
    NotificationHandler<OrderShippedNotification> {
    override suspend fun handle(notification: OrderShippedNotification) {
        if (shouldFail) throw RuntimeException("Push service unavailable")
        println("🔔 PushShipmentHandler: push notification sent for ${notification.orderId}")
    }
}

/** Simulates a flaky analytics sink. */
class AnalyticsShipmentHandler(private val shouldFail: Boolean = false) :
    NotificationHandler<OrderShippedNotification> {
    override suspend fun handle(notification: OrderShippedNotification) {
        if (shouldFail) throw RuntimeException("Analytics service timeout")
        println("📊 AnalyticsShipmentHandler: event tracked for ${notification.orderId}")
    }
}

// ── Registrar ─────────────────────────────────────────────────────────────────

class ShipOrderRegistrar(
    pushFails: Boolean = false,
    analyticsFails: Boolean = false,
) : MediatorRegistrar {
    private val push = PushShipmentHandler(pushFails)
    private val analytics = AnalyticsShipmentHandler(analyticsFails)

    override fun register(registry: HandlerRegistry) {
        registry.scope {
            // Request handler
            +ShipOrderHandler()

            // Exception handlers — registered in specificity order (most specific first)
            registerExceptionHandler(ShipOrderCommand::class, OrderNotFoundException::class, OrderNotFoundExceptionHandler())
            registerExceptionHandler(ShipOrderCommand::class, OutOfStockException::class, OutOfStockExceptionHandler())

            // Notification handlers
            registerNotification(EmailShipmentHandler())
            registerNotification(SmsShipmentHandler())
            registerNotification(push)
            registerNotification(analytics)
        }
    }
}

// ── Demo helpers ──────────────────────────────────────────────────────────────

/**
 * Shows how [AggregateException] surfaces when [ContinueOnExceptionNotificationPublisher]
 * is used: every handler runs regardless of failures, and all exceptions are bundled
 * into one [AggregateException] at the end.
 *
 * Contrast with [com.fajrbahr.mediatork.SequentialNotificationPublisher], which stops
 * at the first failing handler, or [com.fajrbahr.mediatork.ParallelNotificationPublisher],
 * which surfaces only the first exception from concurrent handlers.
 */
suspend fun demoContinueOnException(mediator: Mediator) {
    println(
        """
        |Strategy: ContinueOnExceptionNotificationPublisher
        |  — All handlers run even when some fail.
        |  — Every exception is collected and re-thrown as a single AggregateException.
        """.trimMargin()
    )

    val notification = OrderShippedNotification("ORD-999", "TRK-999")

    runCatching {
        // Override the default publisher just for this call so the demo is self-contained.
        mediator.publish(notification, ContinueOnExceptionNotificationPublisher())
    }.onSuccess {
        println("✅ All handlers succeeded — no AggregateException thrown")
    }.onFailure { throwable ->
        when (throwable) {
            is AggregateException -> {
                // AggregateException.message already contains all failure details,
                // e.g. "2 handler(s) failed: Push service unavailable, Analytics service timeout"
                println("❌ AggregateException caught:")
                println("   ${throwable.message}")
            }
            else -> println("Unexpected error: ${throwable.message}")
        }
    }
}
