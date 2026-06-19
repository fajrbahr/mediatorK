package com.fajrbahr.mediatork.pipeline.buildin

import com.fajrbahr.mediatork.Request
import com.fajrbahr.mediatork.RequestContext
import com.fajrbahr.mediatork.pipeline.PipelineBehavior
import com.fajrbahr.mediatork.pipeline.RequestHandlerDelegate
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.TimeSource

/**
 * A [com.fajrbahr.mediatork.pipeline.PipelineBehavior] that enforces a maximum dispatch rate per request type.
 *
 * Uses a sliding-window counter: at most [maxRequests] requests of the same type are
 * allowed within any [windowMs]-millisecond window. Requests that exceed the limit
 * throw [RateLimitExceededException] immediately — they are never queued.
 *
 * ```kotlin
 * RateLimitPipelineBehavior(maxRequests = 5, windowMs = 1_000) // 5 req/s per type
 * ```
 *
 * @param maxRequests maximum number of allowed dispatches per [windowMs]. Must be ≥ 1.
 * @param windowMs duration of the sliding window in milliseconds. Must be > 0.
 * @param order position in the behavior chain. Defaults to `0`.
 */
class RateLimitPipelineBehavior(
    val maxRequests: Int,
    val windowMs: Long,
    override val order: Int = 0,
) : PipelineBehavior {

    init {
        require(maxRequests >= 1) { "maxRequests must be >= 1, was $maxRequests" }
        require(windowMs > 0) { "windowMs must be > 0, was $windowMs" }
    }

    private val mutex = Mutex()

    /** Per-type ring of timestamps for in-window calls (stored as elapsed ms marks). */
    private val windows = mutableMapOf<String, ArrayDeque<TimeSource.Monotonic.ValueTimeMark>>()

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        val key = request::class.simpleName ?: request::class.toString()
        mutex.withLock {
            val now = TimeSource.Monotonic.markNow()
            val deque = windows.getOrPut(key) { ArrayDeque() }
            // Evict timestamps outside the current window
            while (deque.isNotEmpty() && deque.first().elapsedNow().inWholeMilliseconds > windowMs) {
                deque.removeFirst()
            }
            if (deque.size >= maxRequests) {
                throw RateLimitExceededException(key, maxRequests, windowMs)
            }
            deque.addLast(now)
        }
        return next(request)
    }
}

/**
 * Thrown by [RateLimitPipelineBehavior] when a request type exceeds the allowed rate.
 *
 * @param requestName the simple name of the request class that was rate-limited.
 * @param maxRequests the configured limit.
 * @param windowMs the configured window in milliseconds.
 */
class RateLimitExceededException(
    val requestName: String,
    val maxRequests: Int,
    val windowMs: Long,
) : Exception("Rate limit exceeded for $requestName: max $maxRequests per ${windowMs}ms")
