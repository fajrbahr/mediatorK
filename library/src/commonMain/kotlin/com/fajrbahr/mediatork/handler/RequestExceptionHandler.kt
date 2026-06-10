package com.fajrbahr.mediatork.handler

/**
 * Intercepts a specific exception thrown during the handling of a [Request] and
 * converts it into a valid response instead of propagating the exception.
 *
 * This is the mediator-level equivalent of a catch block: register one to translate
 * domain exceptions into typed responses (e.g. map a `NotFoundException` to an
 * empty result rather than crashing the caller).
 *
 * Only one exception handler per `(request type, exception type)` combination is
 * matched — the first registered handler whose exception class [isInstance] of the
 * thrown exception is used.
 *
 * @param TRequest the request type whose pipeline this handler guards.
 * @param TResponse the response type; must match [TRequest]'s response type parameter.
 * @param TException the exception type this handler handles.
 * @see HandlerRegistry.registerExceptionHandler
 */
interface RequestExceptionHandler<in TRequest : Request<TResponse>, TResponse, in TException : Throwable> {
    /**
     * Converts [exception] thrown while handling [request] into a [TResponse].
     *
     * @param requestContext the context for the current pipeline execution.
     * @param request the request that was being handled when the exception occurred.
     * @param exception the exception that was thrown.
     * @return a fallback response that replaces the failed handler's result.
     */
    suspend fun handle(requestContext: RequestContext, request: TRequest, exception: TException): TResponse
}
