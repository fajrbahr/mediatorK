package com.fajrbahr.mediatork.pipeline.buildin

import com.fajrbahr.mediatork.Request
import com.fajrbahr.mediatork.RequestContext
import com.fajrbahr.mediatork.pipeline.PipelineBehavior
import com.fajrbahr.mediatork.pipeline.RequestHandlerDelegate
import kotlin.time.TimeSource

/**
 * A [com.fajrbahr.mediatork.pipeline.PipelineBehavior] that measures how long each request takes and reports it via a callback.
 *
 * The callback receives the request's class name and the elapsed duration in milliseconds.
 * Timing is always reported — even when the handler throws — so you get latency data for
 * both successful and failed requests.
 *
 *
 * @param onTiming callback invoked after every request with `(requestName, durationMs)`.
 * @param order position in the behavior chain. Defaults to `0`.
 */
class TimingPipelineBehavior(
    override val order: Int = 0,
    val onTiming: (requestName: String, durationMs: Long) -> Unit,
) : PipelineBehavior {

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        val name = request::class.simpleName ?: "UnknownRequest"
        val start = TimeSource.Monotonic.markNow()
        try {
            return next(request)
        } finally {
            onTiming(name, start.elapsedNow().inWholeMilliseconds)
        }
    }
}
