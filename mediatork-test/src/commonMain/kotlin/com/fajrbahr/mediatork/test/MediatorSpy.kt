package com.fajrbahr.mediatork.test

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Notification
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.StreamRequest
import com.fajrbahr.mediatork.notification.NotificationPublishStrategy
import kotlinx.coroutines.flow.Flow

class MediatorSpy(@PublishedApi internal val delegate: Mediator) : Mediator {
    @PublishedApi
    internal val _sent = mutableListOf<Any>()
    val sent: List<Any> get() = _sent.toList()

    @PublishedApi
    internal val _published = mutableListOf<Any>()
    val published: List<Any> get() = _published.toList()

    inline fun <reified T : Request<*>> sentOf(): List<T> =
        _sent.filterIsInstance<T>()

    inline fun <reified T : Notification> publishedOf(): List<T> =
        _published.filterIsInstance<T>()

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Request<*>> handle(noinline block: suspend (T) -> Any?) {
        val fake = delegate as? FakeMediator
            ?: error("handle() requires the delegate to be a FakeMediator")
        fake.handlers[T::class] = { request -> block(request as T) }
    }

    override suspend fun <TRequest : Request<TResult>, TResult> send(request: TRequest): TResult {
        _sent.add(request)
        return delegate.send(request)
    }

    override suspend fun <T : Notification> publish(notification: T) {
        _published.add(notification)
        delegate.publish(notification)
    }

    override suspend fun <T : Notification> publish(notification: T, publisher: NotificationPublishStrategy) {
        _published.add(notification)
        delegate.publish(notification, publisher)
    }

    override fun <TRequest : StreamRequest<T>, T> stream(request: TRequest): Flow<T> {
        _sent.add(request as Any)
        return delegate.stream(request)
    }
}
