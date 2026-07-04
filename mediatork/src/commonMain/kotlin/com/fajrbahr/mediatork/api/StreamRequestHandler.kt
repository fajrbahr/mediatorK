package com.fajrbahr.mediatork.api

import kotlinx.coroutines.flow.Flow

/**
 * Handles a specific [StreamRequest] type.
 */
fun interface StreamRequestHandler<in TRequest : StreamRequest<T>, T> {
    fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: TRequest
    ): Flow<T>
}
