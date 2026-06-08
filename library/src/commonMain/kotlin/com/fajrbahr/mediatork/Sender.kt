package com.fajrbahr.mediatork

/**
 * Capability for dispatching a [Request] to its single registered handler.
 *
 * Implementations are responsible for resolving the correct [RequestHandler],
 * running all registered [PipelineBehavior]s, [RequestPreProcessor]s, and
 * [RequestPostProcessor]s, and returning the handler's result to the caller.
 *
 * @see Mediator
 * @see RequestHandler
 */
interface Sender {
    /**
     * Sends [request] through the full processing pipeline and returns the result.
     *
     * The execution order is:
     * 1. Applicable [PipelineBehavior]s (sorted by [PipelineBehavior.order], outermost first)
     * 2. All [RequestPreProcessor]s (sorted by order)
     * 3. The matched [RequestHandler]
     * 4. All [RequestPostProcessor]s (sorted by order)
     *
     * @param TReq the concrete request type.
     * @param TRes the response type produced by the handler.
     * @param request the request to dispatch.
     * @return the value returned by the matching [RequestHandler].
     * @throws MissingHandlerException if no handler is registered for [TReq].
     */
    suspend fun <TReq : Request<TRes>, TRes> send(request: TReq): TRes
}
