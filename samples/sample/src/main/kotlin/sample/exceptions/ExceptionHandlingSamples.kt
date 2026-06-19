package sample.exceptions

import com.fajrbahr.mediatork.*
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.MediatorRegistrar
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestValidator
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.notification.NotificationPublishStrategy
import com.fajrbahr.mediatork.api.Notification
import com.fajrbahr.mediatork.api.NotificationHandler
import com.fajrbahr.mediatork.validator.ValidationResult

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

// ── Validators ────────────────────────────────────────────────────────────────

class OrderExistsValidator : RequestValidator<ShipOrderCommand> {
    override fun validate(request: ShipOrderCommand): ValidationResult =
        if (request.orderId == "MISSING")
            ValidationResult.Invalid(listOf("Order '${request.orderId}' not found"))
        else
            ValidationResult.Valid
}

class WarehouseInStockValidator : RequestValidator<ShipOrderCommand> {
    override fun validate(request: ShipOrderCommand): ValidationResult =
        if (request.warehouseId == "WH-EMPTY")
            ValidationResult.Invalid(listOf("Warehouse '${request.warehouseId}' is out of stock"))
        else
            ValidationResult.Valid
}

// ── Handler ───────────────────────────────────────────────────────────────────

class ShipOrderHandler : RequestHandler<ShipOrderCommand, ShipmentResult> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: ShipOrderCommand,
    ): ShipmentResult {
        val tracking = "TRK-${request.orderId}-${request.warehouseId}"
        mediator.publish(OrderShippedNotification(request.orderId, tracking))
        return ShipmentResult(trackingNumber = tracking, status = "SHIPPED")
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
            +ShipOrderHandler()

            +OrderExistsValidator()
            +WarehouseInStockValidator()

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
 * Shows how [AggregateException] surfaces when [NotificationPublishStrategy.CONTINUE_ON_EXCEPTION]
 * is used: every handler runs regardless of failures, and all exceptions are bundled
 * into one [AggregateException] at the end.
 *
 * Contrast with [NotificationPublishStrategy.SEQUENTIAL], which stops at the first
 * failing handler, or [NotificationPublishStrategy.PARALLEL], which surfaces only
 * the first exception from concurrent handlers.
 */
suspend fun demoContinueOnException(mediator: Mediator) {
    println(
        """
        |Strategy: NotificationPublishStrategy.CONTINUE_ON_EXCEPTION
        |  — All handlers run even when some fail.
        |  — Every exception is collected and re-thrown as a single AggregateException.
        """.trimMargin()
    )

    val notification = OrderShippedNotification("ORD-999", "TRK-999")

    runCatching {
        // Override the default publisher just for this call so the demo is self-contained.
        mediator.publish(notification, NotificationPublishStrategy.CONTINUE_ON_EXCEPTION)
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
