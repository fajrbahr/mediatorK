package sample.meditor.behaviors

import com.fajrbahr.mediatork.api.PipelineBehavior
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandlerDelegate
import com.fajrbahr.mediatork.api.Stage

class QueryOnlyBehavior : PipelineBehavior {
    override fun appliesTo(request: Request<*>) = request !is Request.Unit

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        println("[QUERY-ONLY] Wrapping: ${request::class.simpleName}")
        return next(request)
    }
}

class DisabledBehavior : PipelineBehavior {
    override val isEnabled = false

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult = error("Should never execute — behavior is disabled")
}

class ResponseObserverBehavior : PipelineBehavior {
    override val stage = Stage.Post

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        val result = next(request)
        println("[POST] ${request::class.simpleName} completed")
        return result
    }
}
