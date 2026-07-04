package dsl.meditor

import com.fajrbahr.mediatork.handler.trySend
import com.fajrbahr.mediatork.mediatorK
import com.fajrbahr.mediatork.missingNotificationHandlerSilent
import com.fajrbahr.mediatork.missingRequestHandlerThrow
import com.fajrbahr.mediatork.notification.NotificationPublishStrategy
import com.fajrbahr.mediatork.pipeline.buildin.loggingPipelineBehavior
import com.fajrbahr.mediatork.validator.ValidationException
import dsl.meditor.behaviors.localeBehavior
import dsl.meditor.behaviors.measurePipelineBehavior
import dsl.meditor.behaviors.streamLoggingBehavior
import dsl.meditor.orders.create.CreateOrderCommand
import dsl.meditor.orders.create.OrderCreatedNotification
import dsl.meditor.orders.create.orderNotificationRegistrar
import dsl.meditor.orders.create.orderRegistrar
import dsl.meditor.orders.delete.DeleteOrderCommand
import dsl.meditor.orders.delete.deleteOrderRegistrar
import dsl.meditor.orders.queries.getorder.getOrderRegistrar
import dsl.meditor.orders.queries.query.GetOrderQuery
import dsl.meditor.orders.stream.orderUpdatesRegistrar
import dsl.meditor.orders.stream.OrderUpdatesStream
import dsl.meditor.price.GetPriceQuery
import dsl.meditor.price.createProductRegistrar
import dsl.meditor.price.WatchPriceQuery
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

private val mediator = mediatorK {

    // ── Product Features ────────────────────────────────────────────────────
    registrar(createProductRegistrar(repo, pushService, inAppService))

    // ── Order Management Features ───────────────────────────────────────────
    registrar(orderRegistrar)
    registrar(orderNotificationRegistrar)
    registrar(getOrderRegistrar)
    registrar(deleteOrderRegistrar)
    registrar(orderUpdatesRegistrar)

    // ── Behaviors (request + stream) ────────────────────────────────────────
    behaviors(
        localeBehavior,
        measurePipelineBehavior,
        loggingPipelineBehavior(),
        streamLoggingBehavior,
    )

    // ── Configuration ───────────────────────────────────────────────────────

    // Notification delivery strategy - Choose one:
    notificationPublisher = NotificationPublishStrategy.SequentialNotificationPublisher()
    // notificationPublisher = NotificationPublishStrategy.ParallelNotificationPublisher()
    // notificationPublisher = NotificationPublishStrategy.fireAndForget(scope)

    // Verify all request types have registered handlers
    verifyHandlers = true

    // Missing request handler strategy - Choose one:
    missingRequestHandler = missingRequestHandlerThrow
    // OR: missingRequestHandler { request -> println("[MISSING] ${request::class.simpleName}"); null }

    // Missing notification handler strategy - Choose one:
    missingNotificationHandler = missingNotificationHandlerSilent
    // missingNotificationHandler = missingNotificationHandlerThrow
    // missingNotificationHandler { notification -> ... custom logic ... }
}

fun main(): Unit = runBlocking {

    // ═══════════════════════════════════════════════════════════════════════════
    // PRODUCT PRICING EXAMPLES
    // ═══════════════════════════════════════════════════════════════════════════

    // ── Mapped Feature with multiple validators + bundled behavior ──────────
    println("=== Mapped Feature: GetPrice ===")
    val price = mediator.send(GetPriceQuery(productId = "PROD-1"))
    println("Price: $price")

    // ── Stream Feature ─────────────────────────────────────────────────────
    println("\n=== Stream Feature: WatchPriceUpdates ===")
    mediator.stream(WatchPriceQuery(productId = "PROD-1")).collect { update ->
        println("  ${update.productId}: ${update.oldCents}c -> ${update.newCents}c")
    }

    println()

    // ═══════════════════════════════════════════════════════════════════════════
    // ORDER MANAGEMENT EXAMPLES
    // ═══════════════════════════════════════════════════════════════════════════

    // ── 1. Command with response ─────────────────────────────────────────────
    println("=== Command: CreateOrder ===")
    val orderResult = mediator.send(
        CreateOrderCommand(id = "1", amount = 150.0)
    )
    println("Order result: $orderResult")

    println()

    // ── 2. Void command (Request.Unit) + fallback handler ────────────────────
    println("=== Void Command: DeleteOrder (active — DB handler) ===")
    mediator.send(DeleteOrderCommand(orderId = "ORD-123"))

    println()

    println("=== Void Command: DeleteOrder (archived — fallback to archive) ===")
    mediator.send(DeleteOrderCommand(orderId = "ARCHIVED-456"))

    println()

    // ── 3. Stream request (Flow-based) ───────────────────────────────────────
    println("=== Stream: OrderUpdates ===")
    mediator.stream(OrderUpdatesStream(orderId = "ORD-9988")).collect { update ->
        println("  [STREAM] ${update.orderId}: ${update.status}")
    }

    println()

    // ── 4. Query with fail-fast validation ───────────────────────────────────
    println("=== Query: GetOrder (valid) ===")
    val order = mediator.send(
        GetOrderQuery(orderId = "ORD-9988", customerId = "USR-42")
    )
    println("Order: $order")

    println()

    println("=== Query: GetOrder (invalid — fail-fast validation) ===")
    runCatching {
        mediator.send(GetOrderQuery(orderId = "9988", customerId = "USR-42"))
    }.onFailure { e ->
        if (e is ValidationException) {
            println("Validation errors: ${e.errors}")
            if (e.warnings.isNotEmpty()) println("Validation warnings: ${e.warnings}")
        }
    }

    println()

    // ── 5. Collect-all validation: errors + warnings together ──────────────
    println("=== Validation: errors + warnings (CreateOrderCommand) ===")
    runCatching {
        mediator.send(CreateOrderCommand(id = "", amount = 8_000.0))
    }.onFailure { e ->
        if (e is ValidationException) {
            println("  Errors:   ${e.errors}")
            println("  Warnings: ${e.warnings}")
        }
    }

    println()

    // ── 6. Validation: warnings only (valid but with caution) ────────────────
    println("=== Validation: warnings only — high-value order passes with warnings ===")
    val highValueResult = mediator.send(
        CreateOrderCommand(id = "2", amount = 7_500.0)
    )
    println("Order result: $highValueResult")

    println()

    // ── 7. Custom publish strategy per-call (fire-and-forget) ────────────────
    println("=== Custom publish strategy: FireAndForget ===")
    mediator.publish(
        OrderCreatedNotification("ORD-FF", "ff@example.com", "+0000", 50.0),
        NotificationPublishStrategy.fireAndForget(this),
    )
    delay(500)

    println()

    // ── 8. Missing handler — silent (no crash) ──────────────────────────────
    println("=== Missing notification handler: silent ===")
    mediator.publish(dsl.meditor.orders.create.UnhandledNotification())
    println("  No crash — SilentMissingNotificationHandler swallowed it")

    println()

    // ── 9. trySend — safe dispatch returning Result<T> ───────────────────────
    println("=== trySend: safe dispatch ===")
    val safeResult = mediator.trySend(CreateOrderCommand(id = "safe-1", amount = 50.0))
    println("  trySend success: ${safeResult.isSuccess}, value: ${safeResult.getOrNull()}")
}
