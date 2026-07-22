package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.*
import kotlinx.coroutines.flow.Flow

@PublishedApi
internal class LazyRequestHandler<TRequest : Request<TResult>, TResult>(
    provider: () -> RequestHandler<TRequest, TResult>,
) : RequestHandler<TRequest, TResult> {

    private val delegate by lazy(provider)

    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: TRequest,
    ): TResult = delegate.handle(mediator, requestContext, request)
}

@PublishedApi
internal class LazyStreamRequestHandler<TRequest : StreamRequest<T>, T>(
    provider: () -> StreamRequestHandler<TRequest, T>,
) : StreamRequestHandler<TRequest, T> {

    private val delegate by lazy(provider)

    override fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: TRequest,
    ): Flow<T> = delegate.handle(mediator, requestContext, request)
}

@PublishedApi
internal class LazyNotificationHandler<T : Notification>(
    override val order: Int = 0,
    provider: () -> NotificationHandler<T>,
) : NotificationHandler<T> {

    private val delegate by lazy(provider)

    override suspend fun handle(notification: T) = delegate.handle(notification)
}
