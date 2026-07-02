package dsl.meditor

import com.fajrbahr.mediatork.handler.trySend
import com.fajrbahr.mediatork.mediatorK
import com.fajrbahr.mediatork.notification.NotificationPublishStrategy
import com.fajrbahr.mediatork.notification.SilentMissingNotificationHandler
import com.fajrbahr.mediatork.pipeline.buildin.LoggingPipelineBehavior
import com.fajrbahr.mediatork.validator.ValidationException
import com.fajrbahr.mediatork.validator.rules
import dsl.meditor.behaviors.LocaleBehavior
import dsl.meditor.behaviors.MeasurePipelineBehaviour
import dsl.meditor.context.locale
import dsl.meditor.orders.create.CreateOrderCommand
import dsl.meditor.orders.create.OrderCreatedNotification
import dsl.meditor.orders.create.OrderResult
import dsl.meditor.orders.create.UnhandledNotification
import dsl.meditor.orders.create.pushWithFallback
import dsl.meditor.orders.delete.DeleteOrderCommand
import dsl.meditor.orders.delete.deleteOrderHandler
import dsl.meditor.orders.queries.GetOrderQuery
import dsl.meditor.orders.queries.OrderDetails
import dsl.meditor.orders.stream.OrderUpdate
import dsl.meditor.orders.stream.OrderUpdatesStream
import dsl.meditor.products.GetPriceQuery
import dsl.meditor.products.GetProductQuery
import dsl.meditor.products.getPriceFeature
import dsl.meditor.products.getProductFeature
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking

private val mediator = mediatorK {

    // ── Inline lambda handler: command with response ────────────────────────
    handle<CreateOrderCommand, OrderResult> { cmd ->
        val newOrderId = "ORD-${cmd.id}"
        val warnings = context.getMetaData<List<*>>("validation_warnings")

        println("Creating order $newOrderId with locale ${context.locale}")
        if (!warnings.isNullOrEmpty()) {
            println("  [WARN] ${warnings.joinToString("; ")}")
        }

        publish(
            OrderCreatedNotification(
                orderId = newOrderId,
                customerEmail = "customer@example.com",
                customerPhone = "+1234567890",
                totalAmount = cmd.amount,
            )
        )

        OrderResult(orderId = newOrderId, responseTime = 0)
    }

    // ── Inline lambda handler: query ────────────────────────────────────────
    handle<GetOrderQuery, OrderDetails> { query ->
        OrderDetails(query.orderId, query.customerId, "CONFIRMED", 99.99)
    }

    // ── Fallback request handler: DB first, then archive (via `otherwise`) ──
    register(deleteOrderHandler)

    // ── Inline lambda stream handler ────────────────────────────────────────
    handleStream<OrderUpdatesStream, OrderUpdate> { req ->
        flow {
            listOf("RECEIVED", "PROCESSING", "SHIPPED", "DELIVERED").forEach { status ->
                delay(100)
                emit(OrderUpdate(req.orderId, status))
            }
        }
    }

    // ── Inline lambda notification handlers (multiple, ordered) ─────────────
    on<OrderCreatedNotification> { n ->
        println("  [EMAIL] Confirmation sent to ${n.customerEmail} for order ${n.orderId}")
    }

    on<OrderCreatedNotification>(order = 1) { n ->
        println("  [SMS] Sent to ${n.customerPhone} for order ${n.orderId}")
    }

    on<OrderCreatedNotification>(order = 2) { n ->
        println("  [AUDIT] Order ${n.orderId} logged, amount: ${n.totalAmount}")
    }

    // ── Fallback notification handler: push first, then in-app ──────────────
    register(pushWithFallback)

    // ── Inline lambda validators ────────────────────────────────────────────
    validate<CreateOrderCommand> { cmd ->
        rules<String> {
            check(cmd.id.isNotBlank()) { "Order ID is required" }
            check(cmd.amount > 0) { "Amount must be positive" }
            check(cmd.amount <= 10_000) { "Amount must not exceed 10,000" }
            warn(cmd.amount > 1_000) { "High-value order — requires manager approval" }
            warn(cmd.amount > 5_000) { "Very high-value order — triggers fraud review" }
        }
    }

    validate<DeleteOrderCommand> { cmd ->
        rules<String> {
            check(cmd.orderId.isNotBlank()) { "Order ID is required" }
        }
    }

    // ── Feature DSL — bundles handler + validator in one unit ────────────────
    +getProductFeature

    // ── Mapped Feature DSL — handler with result mapping via mapper() ───────
    +getPriceFeature

    // ── Pipeline behaviors ──────────────────────────────────────────────────
    behaviors(
        LocaleBehavior(),
        MeasurePipelineBehaviour(),
        LoggingPipelineBehavior(),
    )

    // ── Configuration ───────────────────────────────────────────────────────
    notificationPublisher = NotificationPublishStrategy.SequentialNotificationPublisher()
    missingNotificationHandler = SilentMissingNotificationHandler()
}

fun main(): Unit = runBlocking {

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
    mediator.publish(UnhandledNotification())
    println("  No crash — SilentMissingNotificationHandler swallowed it")

    println()

    // ── 9. trySend — safe dispatch returning Result<T> ───────────────────────
    println("=== trySend: safe dispatch ===")
    val safeResult = mediator.trySend(CreateOrderCommand(id = "safe-1", amount = 50.0))
    println("  trySend success: ${safeResult.isSuccess}, value: ${safeResult.getOrNull()}")

    println()

    // ── 10. Feature DSL ─────────────────────────────────────────────────────
    println("=== Feature: GetProduct ===")
    val product = mediator.send(GetProductQuery(productId = "PROD-1"))
    println("Product: $product")

    println()

    // ── 11. Mapped Feature DSL ──────────────────────────────────────────────
    println("=== Mapped Feature: GetPrice ===")
    val price = mediator.send(GetPriceQuery(productId = "PROD-1"))
    println("Price: $price")
}
