package sample.fallback

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.MediatorRegistrar
import com.fajrbahr.mediatork.api.Notification
import com.fajrbahr.mediatork.api.NotificationHandler
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.handler.otherwise
import com.fajrbahr.mediatork.notification.otherwise
import sample.orders.commands.createorder.CreateOrderCommand
import sample.orders.commands.createorder.OrderResult

// ── Notification ──────────────────────────────────────────────────────────────

data class OrderShippedNotification(val orderId: String, val userId: String) : Notification

// ── Request fallback handlers ─────────────────────────────────────────────────

class LiveCreateOrderHandler : RequestHandler<CreateOrderCommand, OrderResult> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: CreateOrderCommand,
    ): OrderResult {
        throw RuntimeException("Live API is down")
    }
}

class CachedCreateOrderHandler : RequestHandler<CreateOrderCommand, OrderResult> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: CreateOrderCommand,
    ): OrderResult {
        println("  [Cache] Serving order ${request.id} from cache")
        return OrderResult(orderId = request.id, responseIme = 0L)
    }
}

// ── Notification fallback handlers ───────────────────────────────────────────

class PushOrderShippedHandler : NotificationHandler<OrderShippedNotification> {
    override suspend fun handle(notification: OrderShippedNotification) {
        throw RuntimeException("Push service is down")
    }
}

class EmailOrderShippedHandler : NotificationHandler<OrderShippedNotification> {
    override suspend fun handle(notification: OrderShippedNotification) {
        println("  [Email] Fallback: Order ${notification.orderId} shipped for user ${notification.userId}")
    }
}

// ── Registrar ─────────────────────────────────────────────────────────────────

class FallbackRegistrar : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry register (LiveCreateOrderHandler() otherwise CachedCreateOrderHandler())
        registry.registerNotification(PushOrderShippedHandler() otherwise EmailOrderShippedHandler())
    }
}
