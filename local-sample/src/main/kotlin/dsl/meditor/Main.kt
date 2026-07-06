package dsl.meditor

import com.fajrbahr.mediatork.api.Notification
import com.fajrbahr.mediatork.buildMediatorK
import com.fajrbahr.mediatork.handler.trySend
import com.fajrbahr.mediatork.missingNotificationHandlerSilent
import com.fajrbahr.mediatork.missingRequestHandlerThrow
import com.fajrbahr.mediatork.notification.NotificationPublishStrategy
import com.fajrbahr.mediatork.pipeline.buildin.loggingPipelineBehavior
import com.fajrbahr.mediatork.validator.ValidationException
import dsl.meditor.behaviors.localeBehavior
import dsl.meditor.behaviors.measurePipelineBehavior
import dsl.meditor.behaviors.streamLoggingBehavior
import dsl.meditor.orders.advanced.APPROVAL_LEVEL_KEY
import dsl.meditor.orders.advanced.ApproveOrderCommand
import dsl.meditor.orders.advanced.USER_CONTEXT_KEY
import dsl.meditor.orders.advanced.advancedPatternsModule
import dsl.meditor.showcase.comprehensiveShowcaseModule
import dsl.meditor.showcase.ProcessPaymentCommand
import dsl.meditor.showcase.PaymentNotification
import dsl.meditor.showcase.MonitorPaymentStream
import dsl.meditor.showcase.MERCHANT_KEY
import dsl.meditor.showcase.PAYMENT_METHOD_KEY
import dsl.meditor.orders.create.CreateOrderCommand
import dsl.meditor.orders.create.GetOrderQuery
import dsl.meditor.orders.create.OrderCreatedNotification
import dsl.meditor.orders.create.OrderUi
import dsl.meditor.orders.create.getOrderFeature
import dsl.meditor.orders.create.orderFeatureFullyExtracted
import dsl.meditor.orders.delete.DeleteOrderCommand
import dsl.meditor.orders.delete.deleteOrderSlice
import dsl.meditor.orders.stream.OrderUpdatesStream
import dsl.meditor.orders.stream.RealtimeOrderPriceStream
import dsl.meditor.orders.stream.orderUpdatesSlice
import dsl.meditor.orders.stream.orderUpdatesStreamFeatureSlice
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

private val mediator = buildMediatorK {

    // ── Product Features ────────────────────────────────────────────────────
    //  install(productSlice(repo, pushService, inAppService))
    // Feature overloads:
    // - feature<TRequest, TResult>: simple request/response (identity)
    add(getOrderFeature)
    // - feature<TRequest, TRaw, TResult>: with internal result mapping
    add(orderFeatureFullyExtracted)
    // - feature<StreamRequest<T>, T>: stream request handler
    add(orderUpdatesStreamFeatureSlice)
    // - raw handler registration (non-feature)
    add(deleteOrderSlice)
    add(orderUpdatesSlice)

    // ── Advanced DSL patterns ──────────────────────────────────────────────
    // Demonstrates: infix operators, invoke shortcuts, context get/set
    add(advancedPatternsModule)

    // ── COMPREHENSIVE SHOWCASE ─────────────────────────────────────────────
    // Demonstrates ALL library features, extensions, and patterns
    add(comprehensiveShowcaseModule)

    // ── Behaviors (request + stream) ────────────────────────────────────────
    add(
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
    // verifyHandlers = true

    // Missing request handler strategy - Choose one:
    missingRequestHandler = missingRequestHandlerThrow
    // OR: missingRequestHandler { request -> println("[MISSING] ${request::class.simpleName}"); null }

    // Missing notification handler strategy - Choose one:
    missingNotificationHandler = missingNotificationHandlerSilent
    // missingNotificationHandler = missingNotificationHandlerThrow
    // missingNotificationHandler { notification -> ... custom logic ... }


}

fun main(): Unit = runBlocking {

    println()

    // ═══════════════════════════════════════════════════════════════════════════
    // ORDER MANAGEMENT EXAMPLES
    // ═══════════════════════════════════════════════════════════════════════════

    // ═══════════════════════════════════════════════════════════════════════════
    // FEATURE OVERLOAD EXAMPLES
    // ═══════════════════════════════════════════════════════════════════════════

    // ── Feature overload 1: feature<TRequest, TResult> (identity) ──────────────
    println("=== Feature overload 1: Simple request/response (identity) ===")
    val orderInfo = mediator.send(GetOrderQuery(orderId = "ORD-999"))
    println("Order info: $orderInfo")

    println()

    // ── Feature overload 2: feature<TRequest, TRaw, TResult> (with mapping) ────
    println("=== Feature overload 2: With internal result mapping ===")
    val orderUi: OrderUi = mediator.send(
        CreateOrderCommand(id = "1", amount = 150.0)
    )
    println("Order UI: $orderUi")
    println("Order ID: ${orderUi.orderId}")

    println()

    // ── Feature overload 3: feature<StreamRequest<T>, T> ──────────────────────
    println("=== Feature overload 3: Stream feature ===")
    mediator.stream(RealtimeOrderPriceStream(orderId = "ORD-PRICE-1")).collect { update ->
        println("  [STREAM] ${update.orderId}: $${"%.2f".format(update.currentPrice)}")
    }

    println()

    // ═══════════════════════════════════════════════════════════════════════════
    // ADDITIONAL EXAMPLES
    // ═══════════════════════════════════════════════════════════════════════════

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
    val highValueResult: OrderUi = mediator.send(
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
    data class UnhandledNotification(val info: String = "test") : Notification
    mediator.publish(UnhandledNotification())
    println("  No crash — SilentMissingNotificationHandler swallowed it")

    println()

    // ── 9. trySend — safe dispatch returning Result<T> ───────────────────────
    println("=== trySend: safe dispatch ===")
    val safeResult: Result<OrderUi> = mediator.trySend(CreateOrderCommand(id = "safe-1", amount = 50.0))
    println("  trySend success: ${safeResult.isSuccess}, value: ${safeResult.getOrNull()}")

    println()

    // ═══════════════════════════════════════════════════════════════════════════
    // ADVANCED DSL PATTERNS
    // ═══════════════════════════════════════════════════════════════════════════

    // ── Advanced patterns: orElse, context operators, invoke() shorthand ─────
    println("=== Advanced DSL Patterns ===")
    println("Features demonstrated:")
    println("  - feature() invoke shorthand: approvalFeature()")
    println("  - orElse infix operator: handler1 orElse handler2")
    println("  - contextKey<T>(name) for type-safe context access")
    println("  - context[KEY] get/set operators in handlers")

    val approvalResult = mediator.send(ApproveOrderCommand(orderId = "ORD-ADV-1", amount = 5000.0))
    println("Approval result: $approvalResult")

    println()

    // ═══════════════════════════════════════════════════════════════════════════
    // COMPREHENSIVE LIBRARY SHOWCASE - ALL FEATURES
    // ═══════════════════════════════════════════════════════════════════════════

    println("=== Comprehensive Showcase: All Library Features ===")
    println()

    // Feature with ALL extensions: retry(), timeout(), cache(), fallback(), measure(), log()
    println("--- Feature Extensions: retry, timeout, cache, fallback, measure, log ---")
    val paymentRequest = ProcessPaymentCommand(orderId = "PAY-001", amount = 100.50)
    val paymentResult = mediator.send(paymentRequest)
    println("Payment processed: $paymentResult")
    println()

    // Type-safe context keys - contextKey<T>(name)
    println("--- Type-Safe Context Keys (contextKey<T>) ---")
    println("Features demonstrated:")
    println("  - MERCHANT_KEY = contextKey<String>(\"merchant\")")
    println("  - context[MERCHANT_KEY] = \"acme-corp\" (set)")
    println("  - val merchant = context[MERCHANT_KEY] (get)")
    println()

    // Stream handler + feature showcase
    println("--- Stream Handlers & Features ---")
    mediator.stream(MonitorPaymentStream(orderId = "PAY-002")).collect { status ->
        println("  [STREAM] Stage: ${status.stage}")
    }
    println()

    // Notification handlers with fallback chains
    println("--- Notification Handlers with Fallback ---")
    mediator.publish(PaymentNotification(orderId = "PAY-003", amount = 250.0))
    println()

    // Validation patterns
    println("--- Validation Patterns (collecting + short-circuit) ---")
    try {
        mediator.send(ProcessPaymentCommand(orderId = "PAY-004", amount = -100.0))
    } catch (e: Exception) {
        println("Validation error: ${e.message}")
    }
    println()

    // Handler fallback chains with orElse
    println("--- Handler Fallback Chains (orElse) ---")
    val largePayment = ProcessPaymentCommand(orderId = "PAY-BIG", amount = 100_000.0)
    val fallbackResult = mediator.trySend(largePayment)
    println("Fallback result: ${fallbackResult.getOrNull()}")
    println()

    // All features covered
    println("✅ COMPREHENSIVE SHOWCASE COMPLETE")
    println("All library features, extensions, functions, patterns, and utilities demonstrated:")
    println("  ✓ Feature builders with all extensions (retry, timeout, cache, fallback, measure, log)")
    println("  ✓ Context keys and get/set operators")
    println("  ✓ Validators (collecting & short-circuit)")
    println("  ✓ Handler fallback chains (orElse)")
    println("  ✓ Stream handlers and stream features")
    println("  ✓ Notification handlers with fallbacks")
    println("  ✓ Pipeline behaviors (logging, timing, caching, error tracking, etc.)")
    println("  ✓ Notification publish strategies")
    println("  ✓ Result mappers (mapper<TRaw, TResult>)")
    println("  ✓ Handler logging (HandlerLogger)")
    println("  ✓ Custom context storage with type-safe keys")
    println("  ✓ Before/after hooks in features")
    println("  ✓ Dynamic handler registration (mediator.add {})")
    println("  ✓ Safe dispatch (trySend)")
    println("  ✓ Missing handler strategies")

    println()
}
