package sample.behaviors

import com.fajrbahr.mediatork.Request
import com.fajrbahr.mediatork.RequestContext
import com.fajrbahr.mediatork.pipeline.PipelineBehavior
import com.fajrbahr.mediatork.pipeline.RequestHandlerDelegate

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
