package com.fajrbahr.mediatork

/**
 * Handles a specific [Request] type and produces a result.
 *
 * Exactly one handler must be registered per request type. Registering a second
 * handler for the same type silently replaces the first. If no handler is
 * registered, [Sender.send] throws [MissingHandlerException].
 *
 * The [mediator] parameter is injected so that a handler can dispatch secondary
 * requests or publish notifications as part of its own logic without creating
 * a direct dependency on other handlers.
 *
 * @param TRequest the request type this handler processes.
 * @param TResult the type of value produced by this handler.
 * @see RequestPreProcessor
 * @see RequestPostProcessor
 * @see PipelineBehavior
 */
interface RequestHandler<in TRequest : Request<TResult>, TResult> {
    /**
     * Executes the business logic for [request] and returns a result.
     *
     * @param mediator the active mediator, available for issuing nested requests
     *   or publishing notifications from within this handler.
     * @param requestContext mutable bag scoped to this pipeline execution; use it
     *   to read values set by pipeline behaviors or pre-processors.
     * @param request the incoming request to handle.
     * @return the result of processing the request.
     */
    suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: TRequest
    ): TResult
}
