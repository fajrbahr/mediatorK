@file:Suppress("TooGenericExceptionCaught")

package com.fajrbahr.mediatork.handler

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler

/**
 * Tries each handler in [handlers] in order, returning the first successful result.
 * If a handler throws, the exception is swallowed and the next handler is tried.
 * Re-throws the last handler's exception if every handler fails.
 *
 * Compose with [orElse] instead of constructing directly.
 */
internal class FallbackRequestHandler<TRequest : Request<TResult>, TResult>(
    private val handlers: List<RequestHandler<TRequest, TResult>>,
) : RequestHandler<TRequest, TResult> {

    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: TRequest,
    ): TResult {
        var lastException: Throwable? = null
        for (handler in handlers) {
            try {
                return handler.handle(mediator, requestContext, request)
            } catch (e: Throwable) {
                lastException = e
            }
        }
        throw lastException ?: error("FallbackRequestHandler has no handlers")
    }

    internal fun withFallback(handler: RequestHandler<TRequest, TResult>): FallbackRequestHandler<TRequest, TResult> =
        FallbackRequestHandler(handlers + handler)
}

/**
 * Returns a handler that tries `this` first, then [fallback] if `this` throws.
 *
 * Chains naturally: `a otherwise b otherwise c` produces a single [FallbackRequestHandler]
 * with three candidates tried in order.
 */
infix fun <TRequest : Request<TResult>, TResult> RequestHandler<TRequest, TResult>.orElse(
    fallback: RequestHandler<TRequest, TResult>,
): RequestHandler<TRequest, TResult> = when (this) {
    is FallbackRequestHandler -> withFallback(fallback)
    else -> FallbackRequestHandler(listOf(this, fallback))
}
