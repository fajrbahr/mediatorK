package sample.behaviors

import com.fajrbahr.mediatork.api.*

/** Marker interface — apply to requests that require an authorization check. */
interface AuthenticatedRequest

class UnauthorizedException(message: String) : Exception(message)

/**
 * Calls [authorize] for every request that implements [AuthenticatedRequest].
 * Throw [UnauthorizedException] (or any other exception) inside [authorize] to reject the request.
 */
class AuthorizationPipelineBehavior(
    private val authorize: suspend (RequestContext, Request<*>) -> Unit,
) : PipelineBehavior {

    override val stage: Stage get() = Stage.Pre

    override suspend fun <TRequest : Request<TResult>, TResult> process(
        requestContext: RequestContext,
        next: RequestHandlerDelegate<TRequest, TResult>,
        request: TRequest,
    ): TResult {
        if (request is AuthenticatedRequest) authorize(requestContext, request)
        return next(request)
    }
}
