package com.fajrbahr.mediatork.pipeline.buildin

import com.fajrbahr.mediatork.api.PipelineBehavior
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandlerDelegate
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
    ): TResult = withTimeout(timeoutMillis.milliseconds) { next(request) }
}
