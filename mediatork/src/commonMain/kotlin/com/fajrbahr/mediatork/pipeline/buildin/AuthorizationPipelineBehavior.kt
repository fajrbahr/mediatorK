package com.fajrbahr.mediatork.pipeline.buildin

import com.fajrbahr.mediatork.api.PipelineBehavior
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandlerDelegate

/**
 * Marker interface for requests that require authorization.
 *
 * Implement this on any [Request] that should be checked by [AuthorizationPipelineBehavior].
 * Requests that do not implement this interface pass through the behavior untouched.
 *
 */
interface AuthenticatedRequest

/**
 * Thrown by [AuthorizationPipelineBehavior] when authorization fails.
 *
 * @param message description of why authorization was denied.
 */
class UnauthorizedException(message: String = "Unauthorized") : Exception(message)

/**
 * A [PipelineBehavior] that runs an authorization check for every [AuthenticatedRequest].
 *
 * Requests that do not implement [AuthenticatedRequest] are passed through without any check.
 * Authorization logic lives in [authorize] — throw [UnauthorizedException] (or any exception)
 * to deny access; return normally to allow.
 *
 *
 * Populate the `RequestContext` with the token or principal from a preceding
 * [PipelineBehavior] (e.g. one that reads from a `SessionStore` or HTTP header).
 *
 * @param authorize suspend function receiving the [RequestContext] and the request;
 *   throw to deny, return normally to allow.
 * @param order position in the behavior chain. Defaults to `-10` (runs early, after
 *   logging but before business-logic behaviors).
 */
class AuthorizationPipelineBehavior(
    override val order: Int = -10,
    val authorize: suspend (RequestContext, Request<*>) -> Unit,
) : PipelineBehavior {

    override fun appliesTo(request: Request<*>): Boolean = request is AuthenticatedRequest

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        authorize(requestContext, request)
        return next(request)
    }
}
