package com.fajrbahr.mediatork.handler

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.api.StreamRequest
import com.fajrbahr.mediatork.api.StreamRequestHandler
import kotlinx.coroutines.flow.Flow

/**
 * Scope providing access to the mediator for multi-step request handling.
 */
interface HandlerScope {
    val mediator: Mediator
    val requestContext: RequestContext
}

@PublishedApi
internal class HandlerScopeImpl(
    override val mediator: Mediator,
    override val requestContext: RequestContext,
) : HandlerScope

/**
 * Creates a [RequestHandler] via DSL instead of extending a class.
 *
 * @param block handler implementation with access to mediator and request context.
 */
fun <TRequest : Request<TResult>, TResult> handler(
    block: suspend HandlerScope.(TRequest) -> TResult,
): RequestHandler<TRequest, TResult> = RequestHandler { mediator, requestContext, request ->
    val scope = HandlerScopeImpl(mediator, requestContext)
    scope.block(request)
}

/**
 * Creates a [StreamRequestHandler] via DSL instead of extending a class.
 *
 * @param block handler implementation that returns a Flow.
 */
fun <TRequest : StreamRequest<T>, T> streamHandler(
    block: HandlerScope.(TRequest) -> Flow<T>,
): StreamRequestHandler<TRequest, T> = StreamRequestHandler { mediator, requestContext, request ->
    val scope = HandlerScopeImpl(mediator, requestContext)
    scope.block(request)
}
