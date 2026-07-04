package com.fajrbahr.mediatork.pipeline.buildin

import com.fajrbahr.mediatork.api.PipelineBehavior
import com.fajrbahr.mediatork.feature.behavior
import kotlin.time.TimeSource

/**
 * A behavior provider that measures how long each request takes and reports it via a callback.
 *
 * The callback receives the request's class name and the elapsed duration in milliseconds.
 * Timing is always reported — even when the handler throws — so you get latency data for
 * both successful and failed requests.
 *
 * @param onTiming callback invoked after every request with `(requestName, durationMs)`.
 * @param order position in the behavior chain. Defaults to `0`.
 */
fun timingPipelineBehavior(
    order: Int = 0,
    onTiming: (requestName: String, durationMs: Long) -> Unit,
): PipelineBehavior = behavior(order = order) { _, next, request ->
    val name = request::class.simpleName ?: "UnknownRequest"
    val start = TimeSource.Monotonic.markNow()
    try {
        next(request)
    } finally {
        onTiming(name, start.elapsedNow().inWholeMilliseconds)
    }
}
