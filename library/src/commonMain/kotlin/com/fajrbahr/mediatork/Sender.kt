package com.fajrbahr.mediatork
import com.fajrbahr.mediatork.handler.*
import com.fajrbahr.mediatork.pipeline.*

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
     * @param TRequest the concrete request type.
     * @param TResult the response type produced by the handler.
     * @param request the request to dispatch.
     * @return the value returned by the matching [RequestHandler].
     * @throws MissingHandlerException if no handler is registered for [TRequest]. There is
     *   no fallback — a missing request handler is always an error because [send] must
     *   return a typed result and has no safe value to produce.
     */
    suspend fun <TRequest : Request<TResult>, TResult> send(request: TRequest): TResult
}
