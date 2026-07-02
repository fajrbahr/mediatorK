package sample.meditor.behaviors

import com.fajrbahr.mediatork.api.*

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
