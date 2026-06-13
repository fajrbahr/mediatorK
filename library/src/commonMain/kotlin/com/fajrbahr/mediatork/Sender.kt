package com.fajrbahr.mediatork

/**
 * Capability for dispatching a [Request] to its single registered handler.
 *
 * Implementations are responsible for resolving the correct [com.fajrbahr.mediatork.handler.RequestHandler],
 * running all registered [com.fajrbahr.mediatork.pipeline.PipelineBehavior]s, [RequestPreProcessor]s, and
 * [RequestPostProcessor]s, and returning the handler's result to the caller.
 *
 * @see Mediator
 * @see com.fajrbahr.mediatork.handler.RequestHandler
 */
interface Sender {
    /**
     * Sends [request] through the full processing pipeline and returns the result.
     *
     * The execution order is:
     * 1. Applicable [com.fajrbahr.mediatork.pipeline.PipelineBehavior]s (sorted by [com.fajrbahr.mediatork.pipeline.PipelineBehavior.order], outermost first)
     * 2. All [RequestPreProcessor]s (sorted by order)
     * 3. The matched [com.fajrbahr.mediatork.handler.RequestHandler]
     * 4. All [RequestPostProcessor]s (sorted by order)
     *
     * @param TRequest the concrete request type.
     * @param TResult the response type produced by the handler.
     * @param request the request to dispatch.
     * @return the value returned by the matching [com.fajrbahr.mediatork.handler.RequestHandler].
     * @throws MissingHandlerException if no handler is registered for [TRequest]. There is
     *   no fallback — a missing request handler is always an error because [send] must
     *   return a typed result and has no safe value to produce.
     */
    suspend fun <TRequest : Request<TResult>, TResult> send(request: TRequest): TResult
}
