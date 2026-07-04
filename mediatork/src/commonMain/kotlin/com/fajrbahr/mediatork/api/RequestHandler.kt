package com.fajrbahr.mediatork.api

/**
 * Handles a specific [Request] type and produces a result.
 */
fun interface RequestHandler<in TRequest : Request<TResult>, TResult> {
    suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: TRequest
    ): TResult
}
