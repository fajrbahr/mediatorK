package sample.behaviors

import com.fajrbahr.mediatork.PipelineBehavior
import com.fajrbahr.mediatork.Request
import com.fajrbahr.mediatork.RequestContext
import com.fajrbahr.mediatork.RequestHandlerDelegate

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
