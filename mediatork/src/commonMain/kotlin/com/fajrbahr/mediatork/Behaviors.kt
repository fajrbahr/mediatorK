package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

// ── Shared types ─────────────────────────────────────────────────────────────

interface TransactionProvider {
    suspend fun begin()
    suspend fun commit()
    suspend fun rollback()
}

enum class CircuitState { CLOSED, OPEN, HALF_OPEN }

class CircuitOpenException(requestName: String) :
    Exception("Circuit is OPEN for $requestName — request rejected")

class RateLimitExceededException(
    val requestName: String,
    val maxRequests: Int,
    val windowMs: Long,
) : Exception("Rate limit exceeded for $requestName: max $maxRequests per ${windowMs}ms")

private fun Request<*>.name(): String = this::class.simpleName ?: this::class.toString()

// ── Stateless behaviors (functions returning a Behavior) ─────────────────────

fun logging(
    logger: (String) -> Unit = ::println,
    order: Int = -100,
): Behavior = behavior(order = order) { request, _, next ->
    val name = request.name()
    logger("→ $name")
    val result = next()
    logger("← $name result=$result")
    result
}

fun retry(
    maxRetries: Int = 3,
    delayMillis: Long = 0L,
    retryOn: (Throwable) -> Boolean = { true },
    order: Int = 0,
): Behavior {
    require(maxRetries >= 0) { "maxRetries must be >= 0, was $maxRetries" }
    require(delayMillis >= 0) { "delayMillis must be >= 0, was $delayMillis" }
    return behavior(order = order) { _, _, next ->
        var attempt = 0
        while (true) {
            try {
                return@behavior next()
            } catch (e: CancellationException) {
                throw e // never retry cooperative cancellation
            } catch (e: Throwable) {
                if (attempt >= maxRetries || !retryOn(e)) throw e
                attempt++
                if (delayMillis > 0) delay(delayMillis.milliseconds)
            }
        }
        @Suppress("UNREACHABLE_CODE")
        error("unreachable")
    }
}

fun timeout(
    millis: Long,
    order: Int = 0,
): Behavior {
    require(millis > 0) { "millis must be > 0, was $millis" }
    return behavior(order = order) { _, _, next ->
        withTimeout(millis.milliseconds) { next() }
    }
}

fun timing(
    onTiming: (requestName: String, durationMs: Long) -> Unit,
    order: Int = 0,
): Behavior = behavior(order = order) { request, _, next ->
    val name = request.name()
    val start = TimeSource.Monotonic.markNow()
    try {
        next()
    } finally {
        onTiming(name, start.elapsedNow().inWholeMilliseconds)
    }
}

fun errorTracking(
    onError: (request: Request<*>, error: Throwable) -> Unit,
    order: Int = Int.MAX_VALUE,
): Behavior = behavior(order = order) { request, _, next ->
    try {
        next()
    } catch (e: Throwable) {
        onError(request, e)
        throw e
    }
}

fun transaction(
    provider: TransactionProvider,
    order: Int = 0,
): Behavior = behavior(order = order) { _, _, next ->
    provider.begin()
    try {
        val result = next()
        provider.commit()
        result
    } catch (e: Throwable) {
        provider.rollback()
        throw e
    }
}

fun rateLimit(
    maxRequests: Int,
    windowMs: Long,
    order: Int = 0,
): Behavior {
    require(maxRequests >= 1) { "maxRequests must be >= 1, was $maxRequests" }
    require(windowMs > 0) { "windowMs must be > 0, was $windowMs" }
    val mutex = Mutex()
    val windows = mutableMapOf<String, ArrayDeque<TimeSource.Monotonic.ValueTimeMark>>()
    return behavior(order = order) { request, _, next ->
        val key = request.name()
        mutex.withLock {
            val deque = windows.getOrPut(key) { ArrayDeque() }
            while (deque.isNotEmpty() && deque.first().elapsedNow().inWholeMilliseconds > windowMs) {
                deque.removeFirst()
            }
            if (deque.size >= maxRequests) {
                throw RateLimitExceededException(key, maxRequests, windowMs)
            }
            deque.addLast(TimeSource.Monotonic.markNow())
        }
        next()
    }
}

// ── Stateful behaviors (factory → a Behavior subtype carrying a control surface) ──
// Same single shape as the stateless behaviors above: a function returns a [Behavior] value
// that flows straight into `behaviors(...)`. The returned subtype just exposes extra members
// (reset/size/…) delegating to private state held in a companion *State object. No user-facing
// inheritance to extend, no `.behavior` unwrapping — a behavior is always a Behavior value.

// Circuit breaker ────────────────────────────────────────────────────────────

internal class CircuitBreakerState(
    private val failureThreshold: Int,
    private val resetTimeoutMs: Long,
    private val onStateChange: ((CircuitState) -> Unit)?,
) {
    private val mutex = Mutex()
    private var state: CircuitState = CircuitState.CLOSED
    private var failureCount: Int = 0
    private var openedAt: TimeSource.Monotonic.ValueTimeMark? = null

    val currentState: CircuitState get() = state

    suspend fun intercept(request: Request<*>, next: suspend () -> Any?): Any? {
        val name = request.name()

        mutex.withLock {
            when (state) {
                CircuitState.OPEN -> {
                    val elapsed = openedAt?.elapsedNow()?.inWholeMilliseconds ?: Long.MAX_VALUE
                    if (elapsed >= resetTimeoutMs) transitionTo(CircuitState.HALF_OPEN)
                    else throw CircuitOpenException(name)
                }
                CircuitState.CLOSED, CircuitState.HALF_OPEN -> Unit
            }
        }

        return try {
            val result = next()
            mutex.withLock { onSuccess() }
            result
        } catch (e: CancellationException) {
            throw e // cancellation is not a circuit failure
        } catch (e: Throwable) {
            mutex.withLock { onFailure() }
            throw e
        }
    }

    suspend fun reset() = mutex.withLock {
        failureCount = 0
        openedAt = null
        transitionTo(CircuitState.CLOSED)
    }

    private fun onSuccess() {
        if (state == CircuitState.HALF_OPEN || state == CircuitState.OPEN) transitionTo(CircuitState.CLOSED)
        failureCount = 0
    }

    private fun onFailure() {
        failureCount++
        if (state == CircuitState.HALF_OPEN || failureCount >= failureThreshold) {
            transitionTo(CircuitState.OPEN)
            openedAt = TimeSource.Monotonic.markNow()
        }
    }

    private fun transitionTo(next: CircuitState) {
        if (state != next) {
            state = next
            onStateChange?.invoke(next)
        }
    }
}

class CircuitBreakerBehavior internal constructor(
    order: Int,
    private val state: CircuitBreakerState,
) : Behavior(order = order, process = { req, _, next -> state.intercept(req, next) }) {
    val currentState: CircuitState get() = state.currentState
    suspend fun reset() = state.reset()
}

fun circuitBreaker(
    failureThreshold: Int = 5,
    resetTimeoutMs: Long = 10_000L,
    onStateChange: ((CircuitState) -> Unit)? = null,
    order: Int = 0,
): CircuitBreakerBehavior {
    require(failureThreshold >= 1) { "failureThreshold must be >= 1, was $failureThreshold" }
    require(resetTimeoutMs > 0) { "resetTimeoutMs must be > 0, was $resetTimeoutMs" }
    return CircuitBreakerBehavior(order, CircuitBreakerState(failureThreshold, resetTimeoutMs, onStateChange))
}

// Cache ──────────────────────────────────────────────────────────────────────

internal class CacheState(
    private val ttlMs: Long,
    private val keyFor: (Request<*>) -> String,
) {
    private val mutex = Mutex()
    private val entries = mutableMapOf<String, Entry>()

    suspend fun intercept(request: Request<*>, next: suspend () -> Any?): Any? {
        val key = keyFor(request)
        mutex.withLock {
            val entry = entries[key]
            if (entry != null && !entry.isExpired()) return entry.value
        }
        val result = next()
        mutex.withLock { entries[key] = Entry(result, TimeSource.Monotonic.markNow(), ttlMs) }
        return result
    }

    suspend fun invalidate(key: String): Unit = mutex.withLock { entries.remove(key); Unit }
    suspend fun clear() = mutex.withLock { entries.clear() }
    suspend fun size(): Int = mutex.withLock { entries.size }

    private data class Entry(val value: Any?, val mark: TimeSource.Monotonic.ValueTimeMark, val ttlMs: Long) {
        fun isExpired() = mark.elapsedNow().inWholeMilliseconds >= ttlMs
    }
}

class CacheBehavior internal constructor(
    order: Int,
    filter: (Request<*>) -> Boolean,
    private val state: CacheState,
) : Behavior(order = order, appliesTo = filter, process = { req, _, next -> state.intercept(req, next) }) {
    suspend fun invalidate(key: String) = state.invalidate(key)
    suspend fun clear() = state.clear()
    suspend fun size(): Int = state.size()
}

fun cache(
    ttlMs: Long = 60_000L,
    keyFor: (Request<*>) -> String = { it.toString() },
    filter: (Request<*>) -> Boolean = { true },
    order: Int = 0,
): CacheBehavior {
    require(ttlMs > 0) { "ttlMs must be > 0, was $ttlMs" }
    return CacheBehavior(order, filter, CacheState(ttlMs, keyFor))
}

// Deduplicator ─────────────────────────────────────────────────────────────

internal class DeduplicatorState(
    private val keyFor: (Request<*>) -> String,
) {
    private val mutex = Mutex()
    private val inFlight = mutableMapOf<String, CompletableDeferred<Any?>>()

    suspend fun intercept(request: Request<*>, next: suspend () -> Any?): Any? {
        val key = keyFor(request)
        val myDeferred = CompletableDeferred<Any?>()

        val existingDeferred = mutex.withLock {
            inFlight[key].also { if (it == null) inFlight[key] = myDeferred }
        }

        if (existingDeferred != null) return existingDeferred.await()

        return try {
            val result = next()
            myDeferred.complete(result)
            result
        } catch (e: Throwable) {
            myDeferred.completeExceptionally(e)
            throw e
        } finally {
            mutex.withLock { inFlight.remove(key) }
        }
    }

    suspend fun inFlightCount(): Int = mutex.withLock { inFlight.size }
}

class DeduplicatorBehavior internal constructor(
    order: Int,
    private val state: DeduplicatorState,
) : Behavior(order = order, process = { req, _, next -> state.intercept(req, next) }) {
    suspend fun inFlightCount(): Int = state.inFlightCount()
}

/**
 * Collapses concurrent in-flight requests that share a key: while one is running, later
 * requests with the same key await its result instead of invoking the handler again.
 *
 * ⚠️ The default [keyFor] is the request's **class name**, so it deduplicates *all* concurrent
 * calls of the same type regardless of field values — two distinct requests of the same class
 * running at once will share a single result. Override [keyFor] to key by field values (e.g.
 * `{ it.toString() }` for a data class) when that is not what you want.
 */
fun deduplicator(
    keyFor: (Request<*>) -> String = { it::class.simpleName ?: it.toString() },
    order: Int = 0,
): DeduplicatorBehavior = DeduplicatorBehavior(order, DeduplicatorState(keyFor))

// Request counter ────────────────────────────────────────────────────────────

internal class RequestCounterState {
    private val mutex = Mutex()
    private val counts = mutableMapOf<String, Long>()

    suspend fun intercept(request: Request<*>, next: suspend () -> Any?): Any? {
        val key = request.name()
        mutex.withLock { counts[key] = (counts[key] ?: 0L) + 1L }
        return next()
    }

    suspend fun countFor(requestClass: KClass<*>): Long {
        val key = requestClass.simpleName ?: requestClass.toString()
        return mutex.withLock { counts[key] ?: 0L }
    }

    suspend fun snapshot(): Map<String, Long> = mutex.withLock { counts.toMap() }
    suspend fun reset() = mutex.withLock { counts.clear() }
}

class RequestCounterBehavior internal constructor(
    order: Int,
    private val state: RequestCounterState,
) : Behavior(order = order, process = { req, _, next -> state.intercept(req, next) }) {
    suspend fun countFor(requestClass: KClass<*>): Long = state.countFor(requestClass)
    suspend fun snapshot(): Map<String, Long> = state.snapshot()
    suspend fun reset() = state.reset()
}

fun requestCounter(order: Int = 0): RequestCounterBehavior =
    RequestCounterBehavior(order, RequestCounterState())
