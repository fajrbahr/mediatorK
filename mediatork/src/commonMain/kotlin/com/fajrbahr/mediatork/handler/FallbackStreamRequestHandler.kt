@file:Suppress("TooGenericExceptionCaught")

package com.fajrbahr.mediatork.handler

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.StreamRequest
import com.fajrbahr.mediatork.api.StreamRequestHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/**
 * Tries each handler in [handlers] in order, returning the first successful result.
 * If a handler throws, the exception is swallowed and the next handler is tried.
 * Re-throws the last handler's exception if every handler fails.
 *
 * Compose with [orElse] instead of constructing directly.
 */
internal class FallbackStreamRequestHandler<TRequest : StreamRequest<T>, T>(
    private val handlers: List<StreamRequestHandler<TRequest, T>>,
) : StreamRequestHandler<TRequest, T> {

    override fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: TRequest,
    ): Flow<T> {
        var lastException: Throwable? = null
        return handlers.fold<StreamRequestHandler<TRequest, T>, Flow<T>?>(null) { flow, handler ->
            flow?.catch { e ->
                lastException = e
                val nextFlow = handler.handle(mediator, requestContext, request)
                nextFlow
            }
                ?: try {
                    handler.handle(mediator, requestContext, request)
                } catch (e: Throwable) {
                    lastException = e
                    null
                }
        } ?: throw lastException ?: error("FallbackStreamRequestHandler has no handlers")
    }

    internal fun withFallback(handler: StreamRequestHandler<TRequest, T>): FallbackStreamRequestHandler<TRequest, T> =
        FallbackStreamRequestHandler(handlers + handler)
}

/**
 * Returns a handler that tries `this` first, then [fallback] if `this` throws or emits empty.
 *
 * Chains naturally: `a otherwise b otherwise c` produces a single [FallbackStreamRequestHandler]
 * with three candidates tried in order.
 */
infix fun <TRequest : StreamRequest<T>, T> StreamRequestHandler<TRequest, T>.orElse(
    fallback: StreamRequestHandler<TRequest, T>,
): StreamRequestHandler<TRequest, T> = when (this) {
    is FallbackStreamRequestHandler -> withFallback(fallback)
    else -> FallbackStreamRequestHandler(listOf(this, fallback))
}
