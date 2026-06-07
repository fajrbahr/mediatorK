package com.fajrbahr.mediatork

interface RequestExceptionHandler<in TRequest : Request<TResponse>, TResponse, in TException : Throwable> {
    suspend fun handle(requestContext: RequestContext, request: TRequest, exception: TException): TResponse
}
