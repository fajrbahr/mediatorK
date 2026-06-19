package sample.behaviors

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.PipelineBehavior
import com.fajrbahr.mediatork.api.RequestHandlerDelegate


class LoggingBehavior : PipelineBehavior {
    override val order: Int = 1

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest
    ): TResult {
        println("[LOG] --> ${request::class.simpleName}")
        val result = next(request)
        println("[LOG] <-- ${request::class.simpleName} result: $result")
        return result
    }
}
