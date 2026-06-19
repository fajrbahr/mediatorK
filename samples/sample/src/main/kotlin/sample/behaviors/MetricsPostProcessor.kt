package sample.behaviors

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.PipelineBehavior
import com.fajrbahr.mediatork.api.RequestHandlerDelegate

class MetricsBehavior : PipelineBehavior {
    override val tag = PipelineBehavior.Tag.Post

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        val result = next(request)
        println("[METRICS] ${request::class.simpleName} completed")
        return result
    }
}
