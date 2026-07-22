package local.meditor.behaviors

import com.fajrbahr.mediatork.api.PipelineBehavior
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestHandlerDelegate
import kotlin.time.TimeSource

class MeasurePipelineBehavior : PipelineBehavior {
    override val order = 10

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        val start = TimeSource.Monotonic.markNow()
        return try {
            next(request)
        } finally {
            val ms = start.elapsedNow().inWholeMilliseconds
            println("[MEASURE] ${request::class.simpleName} took ${ms}ms")
        }
    }
}
