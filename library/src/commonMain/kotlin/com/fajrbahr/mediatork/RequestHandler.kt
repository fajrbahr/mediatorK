package com.fajrbahr.mediatork

interface RequestHandler<in TRequest : Request<TResult>, TResult> {
    suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: TRequest
    ): TResult
}
