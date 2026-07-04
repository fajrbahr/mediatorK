package com.fajrbahr.mediatork.api

import kotlinx.coroutines.flow.Flow

/**
 * Suspend function alias for the next step in the stream-handling pipeline.
 *
 * A [StreamPipelineBehavior] receives one of these as the `next` parameter;
 * calling it advances execution to the next behavior, or ultimately to the
 * stream handler if there are no more behaviors in the chain.
 *
 * Unlike [RequestHandlerDelegate], this alias is not suspend — it returns a
 * cold [Flow] immediately, matching the non-suspend contract of [StreamRequest] handlers.
 *
 * @param TRequest the stream request type flowing through the pipeline.
 * @param T the type of each item emitted by the flow.
 */
typealias StreamHandlerDelegate<TRequest, T> = (TRequest) -> Flow<T>

/**
 * Cross-cutting concern that wraps stream request handling in a decorator-style chain.
 *
 * [StreamPipelineBehavior] mirrors [PipelineBehavior] but operates on [StreamRequest]s
 * and [Flow]-returning handlers. Because stream handlers return a cold [Flow] immediately
 * without suspending, behaviors here compose flows — they do not await results inline.
 *
 * Create instances via the [streamBehavior] DSL function. Behaviors with a **lower**
 * [order] value are the **outermost** decorators.
 *
 * Typical uses: logging, auth enforcement, rate limiting, and tracing on streams.
 *
 * @see com.fajrbahr.mediatork.feature.streamBehavior
 * @see PipelineBehavior
 */
interface StreamPipelineBehavior : Behavior {
    /**
     * Wraps the downstream stream pipeline step represented by [next].
     *
     * Implementations must call `next(request)` to obtain the downstream [Flow]
     * and return it (possibly transformed). Returning a different flow without
     * calling [next] short-circuits the remaining behaviors and the handler.
     *
     * This function is intentionally **not** suspend — it returns a cold [Flow]
     * immediately, consistent with the [StreamRequestHandler] contract.
     *
     * @param TRequest the concrete stream request type.
     * @param T the type of each emitted item.
     * @param requestContext mutable bag scoped to this stream dispatch.
     * @param next the next step in the pipeline; invoke it to get the downstream flow.
     * @param request the stream request being dispatched.
     * @return a [Flow] that represents the (possibly transformed) stream.
     */
    fun <TRequest : StreamRequest<T>, T> process(
        requestContext: RequestContext,
        next: StreamHandlerDelegate<TRequest, T>,
        request: TRequest,
    ): Flow<T>
}

val StreamPipelineBehavior.order: Int
    get() = (this as? com.fajrbahr.mediatork.feature.LambdaStreamPipelineBehavior)?.order ?: 0

val StreamPipelineBehavior.isEnabled: Boolean
    get() = (this as? com.fajrbahr.mediatork.feature.LambdaStreamPipelineBehavior)?.isEnabled ?: true

fun StreamPipelineBehavior.appliesTo(request: StreamRequest<*>): Boolean =
    (this as? com.fajrbahr.mediatork.feature.LambdaStreamPipelineBehavior)?.appliesTo(request) ?: true
