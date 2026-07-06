package dsl.meditor.showcase

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.Notification
import com.fajrbahr.mediatork.pipeline.buildin.loggingPipelineBehavior
import com.fajrbahr.mediatork.pipeline.buildin.timingPipelineBehavior
import com.fajrbahr.mediatork.pipeline.buildin.timeoutPipelineBehavior
import com.fajrbahr.mediatork.pipeline.buildin.errorTrackingPipelineBehavior
import com.fajrbahr.mediatork.pipeline.buildin.CachingPipelineBehavior
import com.fajrbahr.mediatork.pipeline.buildin.timingPipelineBehavior
import com.fajrbahr.mediatork.feature.behavior
import com.fajrbahr.mediatork.feature.streamBehavior
import com.fajrbahr.mediatork.handler.handler
import com.fajrbahr.mediatork.mediatorModule
import com.fajrbahr.mediatork.notification.NotificationPublishStrategy
import com.fajrbahr.mediatork.missingNotificationHandlerThrow
import com.fajrbahr.mediatork.missingRequestHandlerThrow
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Duration.Companion.seconds

/**
 * COMPREHENSIVE PIPELINE BEHAVIORS & MEDIATOR CONFIGURATION SHOWCASE
 *
 * Demonstrates ALL available:
 * ✅ Pipeline behaviors (request)
 * ✅ Stream pipeline behaviors
 * ✅ Custom behaviors
 * ✅ Notification publish strategies
 * ✅ Missing handler strategies
 * ✅ Mediator configuration options
 * ✅ Handler order and composition
 */

// ────────────────────────────────────────────────────────────────────────────
// REQUEST/RESPONSE FOR BEHAVIORS
// ────────────────────────────────────────────────────────────────────────────

data class ComplexQuery(val id: String) : Request<ComplexResult>
data class ComplexResult(val id: String, val data: String)

data class SimpleEvent(val message: String) : Notification

// ────────────────────────────────────────────────────────────────────────────
// BUILT-IN PIPELINE BEHAVIORS (Request Pipeline)
// ────────────────────────────────────────────────────────────────────────────

// 1. Logging behavior - logs all request/response pairs
val allRequestsLogging = loggingPipelineBehavior()

// 2. Timing behavior - measures execution time
val allRequestsTiming = timingPipelineBehavior { requestName, durationMs ->
    println("[$requestName] took ${durationMs}ms")
}

// 3. Timeout behavior - enforces maximum execution time
val allRequestsTimeout = timeoutPipelineBehavior(timeoutMillis = 5000)

// 4. Error tracking - captures and logs exceptions
val allRequestsErrorTracking = errorTrackingPipelineBehavior { request, error ->
    println("[ERROR] ${request::class.simpleName}: ${error?.message}")
}

// 5. Caching behavior - caches request results
val allRequestsCaching = CachingPipelineBehavior(ttlMs = 60_000L)

// ────────────────────────────────────────────────────────────────────────────
// CUSTOM PIPELINE BEHAVIORS
// ────────────────────────────────────────────────────────────────────────────

// Custom request behavior with lambda
val customRequestBehavior = behavior { ctx, next, request ->
    println("[CUSTOM-REQUEST] Before: ${request::class.simpleName}")
    val result = next(request)
    println("[CUSTOM-REQUEST] After: $result")
    result
}

// Custom stream behavior
val customStreamBehavior = streamBehavior { ctx, next, request ->
    println("[CUSTOM-STREAM] Starting: ${request::class.simpleName}")
    next(request)
}

// ────────────────────────────────────────────────────────────────────────────
// NOTIFICATION PUBLISH STRATEGIES (All patterns)
// ────────────────────────────────────────────────────────────────────────────

// Strategy 1: Sequential - one after another
val sequentialPublisher = NotificationPublishStrategy.SequentialNotificationPublisher()

// Strategy 2: Parallel - all at once
val parallelPublisher = NotificationPublishStrategy.ParallelNotificationPublisher()

// Strategy 3: Fire and forget - async without waiting
suspend fun getFireAndForgetPublisher(scope: CoroutineScope) =
    NotificationPublishStrategy.fireAndForget(scope)

// ────────────────────────────────────────────────────────────────────────────
// HANDLER CONFIGURATIONS
// ────────────────────────────────────────────────────────────────────────────

val demoHandler = handler<ComplexQuery, ComplexResult> { request ->
    println("[HANDLER] Processing ${request.id}")
    ComplexResult(request.id, "processed")
}

// ────────────────────────────────────────────────────────────────────────────
// MEDIATOR MODULE WITH ALL FEATURES
// ────────────────────────────────────────────────────────────────────────────

val allBehaviorsModule = mediatorModule {
    // Register handler
    add(demoHandler)

    // Register all request pipeline behaviors
    add(
        allRequestsLogging,
        allRequestsTiming,
        allRequestsErrorTracking,
        allRequestsCaching,
    )

    // Register custom behaviors
    add(customRequestBehavior)
    add(customStreamBehavior)
}

/**
 * MEDIATOR CONFIGURATION OPTIONS (used in buildMediatorK)
 *
 * Examples of all configuration choices:
 *
 * ```kotlin
 * val mediator = buildMediatorK {
 *     // 1. Add handlers and features
 *     add(handler)
 *     add(feature)
 *     add(mediatorModule)
 *
 *     // 2. Add behaviors
 *     add(loggingPipelineBehavior())
 *     add(timingPipelineBehavior())
 *
 *     // 3. Notification publish strategy (pick one)
 *     notificationPublisher = NotificationPublishStrategy.SequentialNotificationPublisher()
 *     notificationPublisher = NotificationPublishStrategy.ParallelNotificationPublisher()
 *     notificationPublisher = NotificationPublishStrategy.fireAndForget(scope)
 *
 *     // 4. Verify handlers registered (optional)
 *     verifyHandlers = true
 *
 *     // 5. Missing request handler strategy (pick one)
 *     missingRequestHandler = missingRequestHandlerThrow
 *     missingRequestHandler { request -> null }  // custom lambda
 *
 *     // 6. Missing notification handler strategy (pick one)
 *     missingNotificationHandler = missingNotificationHandlerThrow
 *     missingNotificationHandler = missingNotificationHandlerSilent
 *     missingNotificationHandler { notification -> ... }  // custom lambda
 * }
 * ```
 */
