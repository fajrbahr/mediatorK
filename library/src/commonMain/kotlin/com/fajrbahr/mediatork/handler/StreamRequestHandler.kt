package com.fajrbahr.mediatork.handler

import com.fajrbahr.mediatork.Mediator
import com.fajrbahr.mediatork.RequestContext
import com.fajrbahr.mediatork.StreamRequest
import kotlinx.coroutines.flow.Flow

/**
 * Handles a specific [StreamRequest] type and returns a cold [Flow] of results.
 *
 * Unlike [RequestHandler], this interface is not suspend — it returns a cold [Flow]
 * immediately. The actual work begins only when the caller collects the flow.
 * This matches Kotlin's idiom for lazy, incremental sequences.
 *
 * Exactly one handler must be registered per [StreamRequest] type.
 * Dispatching with no handler throws [com.fajrbahr.mediatork.MissingStreamHandlerException].
 *
 * ```kotlin
 * class StreamOrdersHandler(private val repo: OrderRepository)
 *     : StreamRequestHandler<StreamOrdersQuery, Order> {
 *
 *     override fun handle(
 *         mediator: Mediator,
 *         requestContext: RequestContext,
 *         request: StreamOrdersQuery,
 *     ): Flow<Order> = flow {
 *         repo.cursorByCustomer(request.customerId).forEach { emit(it) }
 *     }
 * }
 * ```
 *
 * @param TRequest the stream request type this handler processes.
 * @param T the type of each item emitted.
 * @see StreamRequest
 * @see com.fajrbahr.mediatork.IStreamRequest
 */
interface StreamRequestHandler<in TRequest : StreamRequest<T>, T> {
    /**
     * Returns a cold [Flow] that produces items for [request].
     *
     * @param mediator the active mediator, available for nested sends or publishes.
     * @param requestContext mutable bag scoped to this stream dispatch.
     * @param request the incoming stream request.
     */
    fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: TRequest,
    ): Flow<T>
}
