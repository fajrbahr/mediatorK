package com.fajrbahr.mediatork.pipeline.buildin

import com.fajrbahr.mediatork.Request
import com.fajrbahr.mediatork.RequestContext
import com.fajrbahr.mediatork.pipeline.PipelineBehavior
import com.fajrbahr.mediatork.pipeline.RequestHandlerDelegate

/**
 * Marker interface for requests that require authorization.
 *
 * Implement this on any [Request] that should be checked by [AuthorizationPipelineBehavior].
 * Requests that do not implement this interface pass through the behavior untouched.
 *
 * ```kotlin
 * data class GetOrderQuery(val orderId: String) : Request<Order>, AuthenticatedRequest
 * data class PublicStatusQuery(val id: String) : Request<Status> // no auth needed
 * ```
 */
interface AuthenticatedRequest

/**
 * Thrown by [AuthorizationPipelineBehavior] when authorization fails.
 *
 * @param message description of why authorization was denied.
 */
class UnauthorizedException(message: String = "Unauthorized") : Exception(message)

/**
 * A [com.fajrbahr.mediatork.pipeline.PipelineBehavior] that runs an authorization check for every [AuthenticatedRequest].
 *
 * Requests that do not implement [AuthenticatedRequest] are passed through without any check.
 * Authorization logic lives in [authorize] — throw [UnauthorizedException] (or any exception)
 * to deny access; return normally to allow.
 *
 * ```kotlin
 * AuthorizationPipelineBehavior { context, request ->
 *     val token = context.getMetaDate<String>("token")
 *         ?: throw UnauthorizedException("No token in context")
 *     if (!tokenValidator.isValid(token)) throw UnauthorizedException("Invalid token")
 * }
 * ```
 *
 * Populate the `RequestContext` with the token or principal from a preceding
 * [com.fajrbahr.mediatork.pipeline.PipelineBehavior] (e.g. one that reads from a `SessionStore` or HTTP header).
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
