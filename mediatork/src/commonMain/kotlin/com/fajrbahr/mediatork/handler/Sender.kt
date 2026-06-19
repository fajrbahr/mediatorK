package com.fajrbahr.mediatork.handler

import com.fajrbahr.mediatork.api.Request

/**
 * Capability for dispatching a [Request] to its single registered handler.
 *
 * Implementations are responsible for resolving the correct [com.fajrbahr.mediatork.api.RequestHandler],
 * running all registered [com.fajrbahr.mediatork.api.PipelineBehavior]s in the correct phase order,
 * and returning the handler's result to the caller.
 *
 * @see com.fajrbahr.mediatork.api.Mediator
 * @see com.fajrbahr.mediatork.api.RequestHandler
 * @see com.fajrbahr.mediatork.api.Stage
 */
interface Sender {
    /**
     * Sends [request] through the full processing pipeline and returns the result.
     *
     * Execution order (outermost to innermost):
     * 1. [com.fajrbahr.mediatork.api.Stage.Pre] behaviors (sorted by order)
     * 2. [com.fajrbahr.mediatork.api.Stage.Default] behaviors (sorted by order)
     * 3. [com.fajrbahr.mediatork.api.Stage.Post] behaviors (sorted by order)
     * 4. The matched [com.fajrbahr.mediatork.api.RequestHandler]
     *
     * @param TRequest the concrete request type.
     * @param TResult the response type produced by the handler.
     * @param request the request to dispatch.
     * @return the value returned by the matching [com.fajrbahr.mediatork.api.RequestHandler].
     * @throws com.fajrbahr.mediatork.MissingHandlerException if no handler is registered for [TRequest].
     */
    suspend fun <TRequest : Request<TResult>, TResult> send(request: TRequest): TResult

}

/**
 * Wraps [Sender.send] in a [Result], returning [Result.success] on success and
 * [Result.failure] for any exception — [com.fajrbahr.mediatork.MediatorException] or otherwise.
 */
suspend fun <T> Sender.trySend(request: Request<T>): Result<T> =
    try {
        Result.success(send(request))
    } catch (e: Exception) {
        Result.failure(e)
    }
