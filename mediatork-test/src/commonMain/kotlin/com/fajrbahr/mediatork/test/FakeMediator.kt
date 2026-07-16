package com.fajrbahr.mediatork.test

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Notification
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.StreamRequest
import com.fajrbahr.mediatork.notification.NotificationPublishStrategy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.reflect.KClass

typealias MediatorBuilder = FakeMediatorBuilder

class FakeMediatorBuilder {
    @PublishedApi
    internal val handlers = mutableMapOf<KClass<*>, suspend (Any) -> Any?>()

    lateinit var mediator: FakeMediator
        internal set

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Request<*>> handle(noinline block: suspend (T) -> Any?) {
        handlers[T::class] = { request -> block(request as T) }
    }
}

class FakeMediator(block: FakeMediatorBuilder.() -> Unit = {}) : Mediator {
    @PublishedApi
    internal val handlers = mutableMapOf<KClass<*>, suspend (Any) -> Any?>()

    init {
        val builder = FakeMediatorBuilder()
        builder.block()
        builder.mediator = this
        handlers.putAll(builder.handlers)
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Request<*>> handle(noinline block: suspend (T) -> Any?) {
        handlers[T::class] = { request -> block(request as T) }
    }

    inline fun <reified T : Request<*>> on(): FakeRequestStub<T> =
        FakeRequestStub(T::class, handlers)

    class FakeRequestStub<T : Request<*>>(
        private val type: KClass<T>,
        private val stubs: MutableMap<KClass<*>, suspend (Any) -> Any?>,
    ) {
        infix fun returns(value: Any?) {
            stubs[type] = { value }
        }

        infix fun throws(error: Throwable) {
            stubs[type] = { throw error }
        }

        @Suppress("UNCHECKED_CAST")
        infix fun answers(block: suspend (T) -> Any?) {
            stubs[type] = { request -> block(request as T) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <TRequest : Request<TResult>, TResult> send(request: TRequest): TResult {
        val handler = handlers[request::class]
            ?: error("No handler registered for ${request::class.simpleName}")
        return handler(request) as TResult
    }

    override suspend fun <T : Notification> publish(notification: T) = Unit
    override suspend fun <T : Notification> publish(notification: T, publisher: NotificationPublishStrategy) = Unit
    override fun <TRequest : StreamRequest<T>, T> stream(request: TRequest): Flow<T> = emptyFlow()
}
