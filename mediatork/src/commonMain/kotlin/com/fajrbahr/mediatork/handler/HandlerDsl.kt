package com.fajrbahr.mediatork.handler

import com.fajrbahr.mediatork.HandlerScope
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.api.StreamRequest
import com.fajrbahr.mediatork.api.StreamRequestHandler
import kotlinx.coroutines.flow.Flow

fun <TRequest : Request<TResult>, TResult> handler(
    block: suspend HandlerScope.(TRequest) -> TResult,
): RequestHandler<TRequest, TResult> = RequestHandler { mediator, requestContext, request ->
    HandlerScope(mediator, requestContext).block(request)
}

fun <TRequest : StreamRequest<T>, T> streamHandler(
    block: HandlerScope.(TRequest) -> Flow<T>,
): StreamRequestHandler<TRequest, T> = StreamRequestHandler { mediator, requestContext, request ->
    HandlerScope(mediator, requestContext).block(request)
}
