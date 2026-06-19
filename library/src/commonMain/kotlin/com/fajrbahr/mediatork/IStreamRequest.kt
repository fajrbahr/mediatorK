package com.fajrbahr.mediatork

import kotlinx.coroutines.flow.Flow

/**
 * Capability for dispatching a [StreamRequest] to its single registered handler
 * and receiving the result as a cold [Flow].
 *
 * [stream] is intentionally non-suspend: it resolves the handler and returns a
 * cold [Flow] immediately. The handler's work begins only when the caller
 * collects the flow, keeping dispatch and consumption decoupled.
 *
 * @see Mediator
 * @see StreamRequest
 * @see com.fajrbahr.mediatork.handler.StreamRequestHandler
 */
interface IStreamRequest {
    /**
     * Resolves the handler for [request] and returns a cold [Flow] of results.
     *
     * Nothing executes until the returned flow is collected. Each collection
     * starts a fresh [RequestContext] scoped to that execution.
     *
     * @param TRequest the concrete stream request type.
     * @param T the type of each emitted item.
     * @param request the stream request to dispatch.
     * @return a cold [Flow] that emits the handler's results when collected.
     * @throws MissingStreamHandlerException if no handler is registered for [TRequest].
     */
    fun <TRequest : StreamRequest<T>, T> stream(request: TRequest): Flow<T>
}
