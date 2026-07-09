package com.fajrbahr.mediatork.test

import com.fajrbahr.mediatork.api.*
import com.fajrbahr.mediatork.notification.NotificationPublishStrategy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlin.reflect.KClass

class StubMediator : Mediator {

    @PublishedApi
    internal val requestStubs = mutableMapOf<KClass<*>, (Any) -> Any?>()

    @PublishedApi
    internal val notificationStubs = mutableMapOf<KClass<*>, (Any) -> Unit>()

    @PublishedApi
    internal val streamStubs = mutableMapOf<KClass<*>, (Any) -> Flow<*>>()

    inline fun <reified T : Request<*>> on(): RequestStub<T> =
        RequestStub(T::class, requestStubs)

    inline fun <reified T : Notification> onNotification(): NotificationStub<T> =
        NotificationStub(T::class, notificationStubs)

    inline fun <reified T : StreamRequest<*>> onStream(): StreamStub<T> =
        StreamStub(T::class, streamStubs)

    class RequestStub<T : Request<*>>(
        private val type: KClass<T>,
        private val stubs: MutableMap<KClass<*>, (Any) -> Any?>,
    ) {
        infix fun returns(value: Any?) {
            stubs[type] = { value }
        }

        infix fun throws(error: Throwable) {
            stubs[type] = { throw error }
        }

        @Suppress("UNCHECKED_CAST")
        infix fun answers(block: (T) -> Any?) {
            stubs[type] = { request -> block(request as T) }
        }
    }

    class NotificationStub<T : Notification>(
        private val type: KClass<T>,
        private val stubs: MutableMap<KClass<*>, (Any) -> Unit>,
    ) {
        infix fun throws(error: Throwable) {
            stubs[type] = { throw error }
        }

        @Suppress("UNCHECKED_CAST")
        infix fun answers(block: (T) -> Unit) {
            stubs[type] = { notification -> block(notification as T) }
        }
    }

    class StreamStub<T : StreamRequest<*>>(
        private val type: KClass<T>,
        private val stubs: MutableMap<KClass<*>, (Any) -> Flow<*>>,
    ) {
        infix fun returns(items: List<Any?>) {
            stubs[type] = { flow { items.forEach { emit(it) } } }
        }

        infix fun throws(error: Throwable) {
            stubs[type] = { flow { throw error } }
        }

        @Suppress("UNCHECKED_CAST")
        infix fun answers(block: (T) -> Flow<*>) {
            stubs[type] = { request -> block(request as T) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <TRequest : Request<TResult>, TResult> send(request: TRequest): TResult {
        val stub = requestStubs[request::class]
            ?: error("No stub registered for ${request::class.simpleName}")
        return stub(request) as TResult
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : Notification> publish(notification: T) {
        notificationStubs[notification::class]?.invoke(notification)
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : Notification> publish(notification: T, publisher: NotificationPublishStrategy) {
        notificationStubs[notification::class]?.invoke(notification)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <TRequest : StreamRequest<T>, T> stream(request: TRequest): Flow<T> {
        val stub = streamStubs[request::class]
            ?: return emptyFlow()
        return stub(request) as Flow<T>
    }
}
