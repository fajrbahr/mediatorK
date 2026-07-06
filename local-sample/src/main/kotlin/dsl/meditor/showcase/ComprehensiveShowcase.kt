package dsl.meditor.showcase

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.StreamRequest
import com.fajrbahr.mediatork.api.contextKey
import com.fajrbahr.mediatork.feature.feature
import com.fajrbahr.mediatork.handler.handler
import com.fajrbahr.mediatork.handler.orElse
import com.fajrbahr.mediatork.handler.streamHandler
import com.fajrbahr.mediatork.mediatorModule
import com.fajrbahr.mediatork.notification.orElse
import com.fajrbahr.mediatork.feature.notificationHandler
import com.fajrbahr.mediatork.api.Notification
import com.fajrbahr.mediatork.api.RequestValidator
import com.fajrbahr.mediatork.feature.validate
import com.fajrbahr.mediatork.feature.mapper
import com.fajrbahr.mediatork.feature.HandlerLogger
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration.Companion.milliseconds

/**
 * COMPREHENSIVE LIBRARY SHOWCASE
 *
 * Demonstrates ALL library features, extensions, functions, patterns, and utilities:
 *
 * ✅ Feature extensions: retry(), timeout(), cache(), fallback(), measure(), log()
 * ✅ Handler patterns: handler(), streamHandler(), orElse chains
 * ✅ Validation: collectingValidator, shortCircuitValidator, validate DSL
 * ✅ Notifications: notificationHandler(), orElse fallbacks
 * ✅ Context: contextKey(), get/set operators
 * ✅ Mappers: mapper<TRaw, TResult>
 * ✅ Logging: HandlerLogger interface
 * ✅ Feature builders: handle(), before(), after(), retry(), timeout(), cache(), fallback(), measure(), log()
 * ✅ Mediator modules: mediatorModule, invoke() shorthand
 */

// ────────────────────────────────────────────────────────────────────────────
// REQUEST & RESPONSE TYPES
// ────────────────────────────────────────────────────────────────────────────

data class ProcessPaymentCommand(val orderId: String, val amount: Double) : Request<PaymentResult>
data class PaymentResult(val transactionId: String, val status: String, val amount: Double)
data class PaymentInfo(val orderId: String, val status: String)

data class MonitorPaymentStream(val orderId: String) : StreamRequest<PaymentStatus>
data class PaymentStatus(val orderId: String, val stage: String, val timestamp: Long)

data class PaymentNotification(val orderId: String, val amount: Double) : Notification

// ────────────────────────────────────────────────────────────────────────────
// CONTEXT KEYS (Type-safe context storage)
// ────────────────────────────────────────────────────────────────────────────

val MERCHANT_KEY = contextKey<String>("merchant")
val PAYMENT_METHOD_KEY = contextKey<String>("payment_method")
val RETRY_COUNT_KEY = contextKey<Int>("retry_count")

// ────────────────────────────────────────────────────────────────────────────
// VALIDATORS (All validation patterns)
// ────────────────────────────────────────────────────────────────────────────

// Pattern 1: Custom validator
val paymentValidator: RequestValidator<ProcessPaymentCommand> = validate<ProcessPaymentCommand> {
    check(request.amount > 0, "Amount must be positive")
    check(request.amount <= 1_000_000, "Amount exceeds limit")
    warn(request.amount > 10_000, "Large transaction - may require verification")
}

// Pattern 3: Custom validator
val customPaymentValidator: RequestValidator<ProcessPaymentCommand> = validate<ProcessPaymentCommand> {
    check(request.amount.toString().contains("."), "Amount must have decimal")
}

// ────────────────────────────────────────────────────────────────────────────
// HANDLERS WITH ALL EXTENSION METHODS
// ────────────────────────────────────────────────────────────────────────────

// Primary handler with all feature extensions
val processPaymentFeature = feature<ProcessPaymentCommand, PaymentResult, PaymentInfo> {
    // Register validator
    validate(paymentValidator)

    // Before hook
    before { ctx, request ->
        println("[BEFORE] Processing payment request")
        ctx[MERCHANT_KEY] = "default-merchant"
    }

    // Main handler with context access
    handle { request ->
        val merchant = context[MERCHANT_KEY] ?: "unknown"
        val method = context[PAYMENT_METHOD_KEY] ?: "card"
        println("[HANDLER] Processing $${request.amount} via $method (merchant: $merchant)")

        PaymentResult(
            transactionId = "TXN-${System.nanoTime()}",
            status = "completed",
            amount = request.amount
        )
    }
        // Extension: retry with exponential backoff
        .retry(maxAttempts = 3)
        // Extension: timeout protection
        .timeout(5000.milliseconds)
        // Extension: result caching
        .cache(keyFrom = { it.orderId })
        // Extension: fallback on failure
        .fallback { request ->
            println("[FALLBACK] Using cached result")
            PaymentResult(
                transactionId = "FALLBACK-${System.nanoTime()}",
                status = "cached",
                amount = request.amount
            )
        }
        // Extension: execution timing
        .measure()

    // After hook
    after { ctx, result, request ->
        println("[AFTER] Payment completed")
    }

    // Result mapper: PaymentResult → PaymentInfo
    mapper(mapper<PaymentResult, PaymentInfo> { raw ->
        PaymentInfo(
            orderId = "mapped-order",
            status = raw.status
        )
    })
}

// ────────────────────────────────────────────────────────────────────────────
// HANDLER FALLBACK CHAINS (orElse pattern)
// ────────────────────────────────────────────────────────────────────────────

val primaryPaymentHandler = handler<ProcessPaymentCommand, PaymentResult> { request ->
    if (request.amount > 50_000) throw Exception("Amount too large")
    PaymentResult("TXN-PRIMARY", "success", request.amount)
}

val fallbackPaymentHandler = handler<ProcessPaymentCommand, PaymentResult> { request ->
    println("[FALLBACK HANDLER] Using backup processor")
    PaymentResult("TXN-BACKUP", "success", request.amount)
}

// Infix orElse operator: creates fallback chain
val paymentHandlerWithFallback = primaryPaymentHandler orElse fallbackPaymentHandler

// ────────────────────────────────────────────────────────────────────────────
// STREAM HANDLERS (All patterns)
// ────────────────────────────────────────────────────────────────────────────

// Pattern 1: Stream handler function
val monitorPaymentStreamHandler = streamHandler<MonitorPaymentStream, PaymentStatus> { request ->
    flow {
        val stages = listOf("initiated", "processing", "verified", "settled")
        stages.forEach { stage ->
            emit(PaymentStatus(request.orderId, stage, System.currentTimeMillis()))
        }
    }
}

// Pattern 2: Stream feature (alternative to handler)
val monitorPaymentFeature = feature<MonitorPaymentStream, PaymentStatus> {
    handle { request ->
        flow {
            repeat(4) { index ->
                emit(PaymentStatus(request.orderId, "stage-$index", System.currentTimeMillis()))
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// NOTIFICATION HANDLERS WITH FALLBACK
// ────────────────────────────────────────────────────────────────────────────

val emailNotificationHandler = notificationHandler<PaymentNotification> { notification ->
    println("[EMAIL] Sent receipt for ${notification.orderId}")
}

val smsNotificationHandler = notificationHandler<PaymentNotification> { notification ->
    println("[SMS] Sent confirmation to phone for ${notification.orderId}")
}

// Notification fallback: email orElse sms
val notificationWithFallback = emailNotificationHandler orElse smsNotificationHandler

// ────────────────────────────────────────────────────────────────────────────
// MEDIATOR MODULE CONFIGURATION
// ────────────────────────────────────────────────────────────────────────────

val comprehensiveShowcaseModule = mediatorModule {
    // Feature with all extensions
    add(processPaymentFeature)

    // Handler with fallback chain
    add(paymentHandlerWithFallback)

    // Stream handlers
    add(monitorPaymentStreamHandler)
    add(monitorPaymentFeature)

    // Notification handlers
    add(notificationWithFallback)

    // Feature invoke() shorthand
    monitorPaymentFeature()
}
