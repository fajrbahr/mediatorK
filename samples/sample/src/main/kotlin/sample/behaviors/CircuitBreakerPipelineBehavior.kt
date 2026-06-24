package sample.behaviors

import com.fajrbahr.mediatork.api.*

enum class CircuitState { CLOSED, OPEN, HALF_OPEN }

/**
 * Trips open after [failureThreshold] consecutive failures, preventing calls to a broken
 * downstream. Resets to HALF_OPEN after [resetTimeoutMs] milliseconds, then fully closes on
 * the next success.
 */
class CircuitBreakerPipelineBehavior(
    private val failureThreshold: Int = 5,
    private val resetTimeoutMs: Long = 1_000,
    private val onStateChange: ((CircuitState) -> Unit)? = null,
) : PipelineBehavior {

    @Volatile private var state = CircuitState.CLOSED
    @Volatile private var failureCount = 0
    @Volatile private var lastFailureTime = 0L

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        if (state == CircuitState.OPEN) {
            val elapsed = System.currentTimeMillis() - lastFailureTime
            if (elapsed >= resetTimeoutMs) {
                transition(CircuitState.HALF_OPEN)
            } else {
                throw RuntimeException("Circuit breaker is OPEN — upstream failure detected")
            }
        }

        return try {
            val result = next(request)
            if (state == CircuitState.HALF_OPEN) {
                failureCount = 0
                transition(CircuitState.CLOSED)
            }
            result
        } catch (e: Throwable) {
            lastFailureTime = System.currentTimeMillis()
            failureCount++
            if (failureCount >= failureThreshold) transition(CircuitState.OPEN)
            throw e
        }
    }

    private fun transition(newState: CircuitState) {
        state = newState
        onStateChange?.invoke(newState)
    }
}
