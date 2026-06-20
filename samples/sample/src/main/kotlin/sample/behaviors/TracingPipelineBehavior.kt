package sample.behaviors

import com.fajrbahr.mediatork.api.PipelineBehavior
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandlerDelegate
import java.util.UUID

class TracingPipelineBehavior : PipelineBehavior {
    override val order = -50

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        val traceId = UUID.randomUUID().toString().take(8)
        requestContext.put("traceId", traceId)
        println("[TRACE] $traceId → ${request::class.simpleName}")
        return next(request)
    }
}
