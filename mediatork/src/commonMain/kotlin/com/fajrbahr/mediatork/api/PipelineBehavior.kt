package com.fajrbahr.mediatork.api

/**
 * Suspend function alias for the next step in the request-handling pipeline.
 *
 * A [PipelineBehavior] receives one of these as the `next` parameter; calling it
 * advances execution to the next behavior, or ultimately to the handler if there
 * are no more behaviors in the chain.
 *
 * @param TRequest the request type flowing through the pipeline.
 * @param TResult the response type produced at the end of the pipeline.
 */
typealias RequestHandlerDelegate<TRequest, TResult> = suspend (TRequest) -> TResult

/**
 * Cross-cutting concern that wraps request handling in a decorator-style chain.
 *
 * Behaviors are sorted by [order] — lower values run outermost (first).
 * Create instances via the [behavior][com.fajrbahr.mediatork.feature.behavior] DSL function.
 *
 * @see com.fajrbahr.mediatork.feature.behavior
 */
interface PipelineBehavior : Behavior {
    /**
     * Wraps the downstream pipeline step represented by [next].
     *
     * Implementations must call `next(request)` to continue the chain.
     * Returning without calling [next] short-circuits the remaining behaviors and handler.
     */
    suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult
}

val PipelineBehavior.order: Int
    get() = (this as? com.fajrbahr.mediatork.feature.LambdaPipelineBehavior)?.order ?: 0

val PipelineBehavior.isEnabled: Boolean
    get() = (this as? com.fajrbahr.mediatork.feature.LambdaPipelineBehavior)?.isEnabled ?: true

fun PipelineBehavior.appliesTo(request: Request<*>): Boolean =
    (this as? com.fajrbahr.mediatork.feature.LambdaPipelineBehavior)?.appliesTo(request) ?: true
