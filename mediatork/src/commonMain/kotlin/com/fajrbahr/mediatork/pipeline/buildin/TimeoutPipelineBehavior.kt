package com.fajrbahr.mediatork.pipeline.buildin

import com.fajrbahr.mediatork.api.PipelineBehavior
import com.fajrbahr.mediatork.feature.behavior
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds

/**
 * A behavior provider that cancels the downstream pipeline if it does not complete
 * within [timeoutMillis] milliseconds.
 *
 * Throws [TimeoutCancellationException] (a [CancellationException]) when the deadline
 * is exceeded. Pair with [RetryPipelineBehavior] (at a lower `order` value) if you
 * want to retry timed-out requests.
 *
 * @param timeoutMillis maximum allowed duration in milliseconds. Must be > 0.
 * @param order position in the behavior chain. Defaults to `0`.
 */
fun timeoutPipelineBehavior(
    timeoutMillis: Long,
    order: Int = 0,
): PipelineBehavior {
    require(timeoutMillis > 0) { "timeoutMillis must be > 0, was $timeoutMillis" }
    return behavior(order = order) { _, next, request ->
        withTimeout(timeoutMillis.milliseconds) { next(request) }
    }
}
