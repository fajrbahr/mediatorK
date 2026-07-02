package sample.meditor

import com.fajrbahr.mediatork.handler.trySend
import com.fajrbahr.mediatork.mediatorK
import com.fajrbahr.mediatork.notification.NotificationPublishStrategy
import com.fajrbahr.mediatork.notification.SilentMissingNotificationHandler
import com.fajrbahr.mediatork.pipeline.buildin.LoggingPipelineBehavior
import com.fajrbahr.mediatork.validator.ValidationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import sample.meditor.behaviors.LocaleBehavior
import sample.meditor.behaviors.MeasurePipelineBehaviour
import sample.meditor.orders.create.*
import sample.meditor.orders.delete.DeleteOrderCommand
import sample.meditor.orders.delete.DeleteOrderRegistrar
import sample.meditor.orders.queries.getorder.GetOrderRegistrar
import sample.meditor.orders.queries.query.GetOrderQuery
import sample.meditor.orders.stream.OrderUpdatesRegistrar
import sample.meditor.orders.stream.OrderUpdatesStream

private val mediator = mediatorK {
    registrars(
        OrderRegistrar(),
        OrderNotificationRegistrar(),
        GetOrderRegistrar(),
        DeleteOrderRegistrar(),
        OrderUpdatesRegistrar(),
    )

    behaviors(
        LocaleBehavior(),
        MeasurePipelineBehaviour(),
        LoggingPipelineBehavior(),
    )

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
}
