package sample.fallback

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.Mediator
import com.fajrbahr.mediatork.MediatorRegistrar
import com.fajrbahr.mediatork.RequestContext
import com.fajrbahr.mediatork.handler.RequestHandler
import com.fajrbahr.mediatork.handler.otherwise
import com.fajrbahr.mediatork.notification.Notification
import com.fajrbahr.mediatork.notification.NotificationHandler
import com.fajrbahr.mediatork.notification.otherwise
import sample.orders.commands.createorder.CreateOrderCommand
import sample.orders.commands.createorder.OrderResult

// ── APIs ──────────────────────────────────────────────────────────────────────

/** Simulates a live API that is currently down. */
class LiveOrderApi {
    suspend fun createOrder(id: String): OrderResult {
        println("  [LiveOrderApi] ❌ service unavailable")
        throw RuntimeException("Live API is down")
    }
}

/** Simulates a cache that has the order data. */
class CachedOrderApi {
    suspend fun createOrder(id: String): OrderResult {
        println("  [CachedOrderApi] ✅ served from cache")
        return OrderResult(orderId = "$id-cached", cart = listOf("item-A", "item-B"), responseIme = 0)
    }
}

// ── Request handlers ──────────────────────────────────────────────────────────

class LiveCreateOrderHandler(private val api: LiveOrderApi) : RequestHandler<CreateOrderCommand, OrderResult> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: CreateOrderCommand
    ): OrderResult =
        api.createOrder(request.id)
}

class CachedCreateOrderHandler(private val api: CachedOrderApi) : RequestHandler<CreateOrderCommand, OrderResult> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: CreateOrderCommand
    ): OrderResult =
        api.createOrder(request.id)
}

class StubCreateOrderHandler : RequestHandler<CreateOrderCommand, OrderResult> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: CreateOrderCommand
    ): OrderResult {
        println("  [StubCreateOrderHandler] ✅ stub response")
        return OrderResult(orderId = "${request.id}-stub", responseIme = 0)
    }
}

// ── Notification handlers ─────────────────────────────────────────────────────

data class OrderShippedNotification(val orderId: String, val userId: String) : Notification

class PushShippedHandler : NotificationHandler<OrderShippedNotification> {
    override suspend fun handle(notification: OrderShippedNotification) {
        println("  [PushShippedHandler] ❌ push service unavailable")
        throw RuntimeException("Push service is down")
    }
}

class EmailShippedHandler : NotificationHandler<OrderShippedNotification> {
    override suspend fun handle(notification: OrderShippedNotification) {
        println("  [EmailShippedHandler] ✅ email sent for order ${notification.orderId}")
    }
}

class SmsShippedHandler : NotificationHandler<OrderShippedNotification> {
    override suspend fun handle(notification: OrderShippedNotification) {
        println("  [SmsShippedHandler] ✅ SMS sent for order ${notification.orderId}")
    }
}

// ── Registrar ─────────────────────────────────────────────────────────────────

/**
 * Demonstrates [otherwise] chains for both requests and notifications.
 *
 * Request:      LiveCreateOrderHandler → CachedCreateOrderHandler → StubCreateOrderHandler
 * Notification: PushShippedHandler     → EmailShippedHandler
 *               (both fail)            → SmsShippedHandler  (added as second otherwise)
 */
class FallbackRegistrar : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.scope {
            // Request fallback chain — live API → cache → stub
            +(
                    LiveCreateOrderHandler(LiveOrderApi())
                            otherwise CachedCreateOrderHandler(CachedOrderApi())
                            otherwise StubCreateOrderHandler()
                    )

            // Notification fallback chain — push → email → SMS
            registerNotification<OrderShippedNotification>(
                PushShippedHandler()
                        otherwise EmailShippedHandler()
                        otherwise SmsShippedHandler()
            )
        }
    }
}
