package com.fajrbahr.mediatork.pipeline
import com.fajrbahr.mediatork.handler.*

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

/**
 * A [PipelineBehavior] that cancels the downstream pipeline if it does not complete
 * within [timeoutMillis] milliseconds.
 *
 * Throws [TimeoutCancellationException] (a [CancellationException]) when the deadline
 * is exceeded. Pair with [RetryPipelineBehavior] (at a lower [order] value) if you
 * want to retry timed-out requests.
 *
 * ```kotlin
 * // Cancel any request that takes longer than 5 seconds.
 * val timeout = TimeoutPipelineBehavior(timeoutMillis = 5_000)
 * ```
 *
 * @param timeoutMillis maximum allowed duration in milliseconds. Must be > 0.
 * @param order position in the behavior chain. Defaults to `0`.
 */
class TimeoutPipelineBehavior(
    val timeoutMillis: Long,
    override val order: Int = 0,
) : PipelineBehavior {

    init {
        require(timeoutMillis > 0) { "timeoutMillis must be > 0, was $timeoutMillis" }
    }

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult = withTimeout(timeoutMillis) { next(request) }
}
