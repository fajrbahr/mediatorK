package com.opentool.mediatork.com.opentool.mediatork

interface RequestHandler<in TRequest : Request<TResult>, TResult> {
    suspend fun handle(requestContext: RequestContext, request: TRequest): TResult
}
