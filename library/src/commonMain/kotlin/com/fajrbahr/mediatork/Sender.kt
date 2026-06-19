package com.fajrbahr.mediatork

/**
 * Capability for dispatching a [Request] to its single registered handler.
 *
 * Implementations are responsible for resolving the correct [com.fajrbahr.mediatork.handler.RequestHandler],
 * running all registered [com.fajrbahr.mediatork.pipeline.PipelineBehavior]s in the correct phase order,
 * and returning the handler's result to the caller.
 *
 * @see Mediator
 * @see com.fajrbahr.mediatork.handler.RequestHandler
 * @see com.fajrbahr.mediatork.pipeline.PipelineBehavior.Tag
 */
interface Sender {
    /**
     * Sends [request] through the full processing pipeline and returns the result.
     *
     * Execution order (outermost to innermost):
     * 1. [com.fajrbahr.mediatork.pipeline.PipelineBehavior.Tag.PRE] behaviors (sorted by order)
     * 2. [com.fajrbahr.mediatork.pipeline.PipelineBehavior.Tag.DEFAULT] behaviors (sorted by order)
     * 3. [com.fajrbahr.mediatork.pipeline.PipelineBehavior.Tag.POST] behaviors (sorted by order)
     * 4. The matched [com.fajrbahr.mediatork.handler.RequestHandler]
     *
     * @param TRequest the concrete request type.
     * @param TResult the response type produced by the handler.
     * @param request the request to dispatch.
     * @return the value returned by the matching [com.fajrbahr.mediatork.handler.RequestHandler].
     * @throws MissingHandlerException if no handler is registered for [TRequest].
     */
    suspend fun <TRequest : Request<TResult>, TResult> send(request: TRequest): TResult

}

/**
 * Wraps [Sender.send] in a [Result], returning [Result.success] on success and
 * [Result.failure] for any exception — [MediatorException] or otherwise.
 */
suspend fun <T> Sender.trySend(request: Request<T>): Result<T> =
    try { Result.success(send(request)) }
    catch (e: Exception) { Result.failure(e) }
