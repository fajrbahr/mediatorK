package com.fajrbahr.mediatork.pipeline

import com.fajrbahr.mediatork.Request
import com.fajrbahr.mediatork.RequestContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.TimeSource

/**
 * State of a [CircuitBreakerPipelineBehavior].
 *
 * - **CLOSED** — normal operation; requests flow through.
 * - **OPEN** — circuit tripped; requests fast-fail with [CircuitOpenException] until the
 *   cooldown expires.
 * - **HALF_OPEN** — one probe request is allowed through; if it succeeds the circuit closes,
 *   if it fails the circuit re-opens.
 */
enum class CircuitState { CLOSED, OPEN, HALF_OPEN }

/**
 * Thrown by [CircuitBreakerPipelineBehavior] when the circuit is [CircuitState.OPEN].
 *
 * @param requestName the class name of the request that was rejected.
 */
class CircuitOpenException(requestName: String) :
    Exception("Circuit is OPEN for $requestName — request rejected")

/**
 * A [PipelineBehavior] that implements the circuit-breaker resilience pattern.
 *
 * After [failureThreshold] consecutive failures the circuit trips to **OPEN** and
 * subsequent requests are rejected immediately with [CircuitOpenException], sparing
 * the downstream service from further load. After [resetTimeoutMs] milliseconds the
 * circuit transitions to **HALF_OPEN** and allows one probe request through:
 *
 * - Probe succeeds → circuit closes, failure counter resets.
 * - Probe fails → circuit re-opens, cooldown restarts.
 *
 * ```
 * CLOSED ──(failureThreshold reached)──► OPEN
 *   ▲                                      │
 *   └──── probe success ◄─ HALF_OPEN ◄─────┘
 *                              │
 *                        probe failure
 *                              │
 *                           re-OPEN
 * ```
 *
 * ```kotlin
 * CircuitBreakerPipelineBehavior(
 *     failureThreshold = 5,
 *     resetTimeoutMs = 10_000,
 * )
 * ```
 *
 * A single instance tracks state for **all** request types. Create one instance per
 * request type (or service boundary) if you need isolated breakers.
 *
 * @param failureThreshold consecutive failures before tripping the circuit. Must be ≥ 1.
 * @param resetTimeoutMs how long to stay OPEN before transitioning to HALF_OPEN. Must be > 0.
 * @param onStateChange optional callback invoked whenever the circuit changes state.
 * @param order position in the behavior chain. Defaults to `0`.
 */
class CircuitBreakerPipelineBehavior(
    val failureThreshold: Int = 5,
    val resetTimeoutMs: Long = 10_000L,
    val onStateChange: ((CircuitState) -> Unit)? = null,
    override val order: Int = 0,
) : PipelineBehavior {

    init {
        require(failureThreshold >= 1) { "failureThreshold must be >= 1, was $failureThreshold" }
        require(resetTimeoutMs > 0) { "resetTimeoutMs must be > 0, was $resetTimeoutMs" }
    }

    private val mutex = Mutex()
    private var state: CircuitState = CircuitState.CLOSED
    private var failureCount: Int = 0
    private var openedAt: TimeSource.Monotonic.ValueTimeMark? = null

    /** The current circuit state. */
    val currentState: CircuitState get() = state

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        val name = request::class.simpleName ?: "UnknownRequest"

        mutex.withLock {
            when (state) {
                CircuitState.OPEN -> {
                    val elapsed = openedAt?.elapsedNow()?.inWholeMilliseconds ?: Long.MAX_VALUE
                    if (elapsed >= resetTimeoutMs) {
                        transitionTo(CircuitState.HALF_OPEN)
                    } else {
                        throw CircuitOpenException(name)
                    }
                }

                CircuitState.CLOSED, CircuitState.HALF_OPEN -> Unit
            }
        }

        return try {
            val result = next(request)
            mutex.withLock { onSuccess() }
            result
        } catch (e: Throwable) {
            mutex.withLock { onFailure() }
            throw e
        }
    }

    private fun onSuccess() {
        if (state == CircuitState.HALF_OPEN || state == CircuitState.OPEN) {
            transitionTo(CircuitState.CLOSED)
        }
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

    /** Manually resets the circuit to CLOSED and clears the failure counter. */
    suspend fun reset() = mutex.withLock {
        failureCount = 0
        openedAt = null
        transitionTo(CircuitState.CLOSED)
    }
}
