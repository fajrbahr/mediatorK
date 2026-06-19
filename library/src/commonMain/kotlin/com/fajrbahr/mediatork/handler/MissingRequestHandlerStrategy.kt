package com.fajrbahr.mediatork.handler

import com.fajrbahr.mediatork.MissingHandlerException
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler

/**
 * Throws [MissingHandlerException] when a request is sent with no registered handler.
 *
 * This is the default missing-request handler passed to [com.fajrbahr.mediatork.MediatorFactory.create].
 * It surfaces misconfiguration immediately rather than silently dropping requests.
 */
class ThrowMissingRequestHandler<TRequest : Request<TResult>, TResult> : RequestHandler<TRequest, TResult> {
    override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: TRequest): TResult {
        throw MissingHandlerException(requestTypeName = request::class.simpleName ?: "Unknown")
    }
}

/**
 * Does nothing and returns [default] when a request is sent with no registered handler.
 *
 * Only use this when unhandled requests are intentional. Misconfiguration
 * will produce no error and no trace — silent data loss.
 */
class SilentMissingRequestHandler<TRequest : Request<TResult>, TResult>(
    private val default: TResult,
) : RequestHandler<TRequest, TResult> {
    override suspend fun handle(mediator: Mediator, requestContext: RequestContext, request: TRequest): TResult = default
}
