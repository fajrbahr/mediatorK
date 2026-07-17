package com.fajrbahr.mediatork.test

import com.fajrbahr.mediatork.Behavior
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Notification
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.StreamRequest
import com.fajrbahr.mediatork.notification.NotificationPublishStrategy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlin.reflect.KClass

/**
 * A configurable [Mediator] test double: stub what `send`/`publish`/`stream` return, then assert
 * on what was [sent] / [published]. Optionally run behaviors around stubs with [onPipeline].
 *
 * ```kotlin
 * val mediator = StubMediator()
 * mediator.on<GetUserQuery>() returns "user:42"
 * mediator.on<CreateOrderCommand>() answers { "order:${it.id}" }
 * ```
 *
 * For the *real* pipeline with real handler lambdas, use `mediatorK { }` directly.
 */
class StubMediator : Mediator {

    @PublishedApi
    internal val requestStubs = mutableMapOf<KClass<*>, (Any) -> Any?>()

    @PublishedApi
    internal val notificationStubs = mutableMapOf<KClass<*>, (Any) -> Unit>()

    @PublishedApi
    internal val streamStubs = mutableMapOf<KClass<*>, (Any) -> Flow<*>>()

    private val pipelineStubs = mutableListOf<PipelineStub>()

    @PublishedApi
    internal val _sent = mutableListOf<Any>()
    val sent: List<Any> get() = _sent.toList()

    private val _published = mutableListOf<Any>()
    val published: List<Any> get() = _published.toList()

    inline fun <reified T : Request<*>> sentOf(): List<T> =
        _sent.filterIsInstance<T>()

    var pipelineEnabled: Boolean = true

    inline fun <reified T : Request<*>> on(): RequestStub<T> =
        RequestStub(T::class, requestStubs)

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Request<*>> on(noinline block: (T) -> Any?) {
        requestStubs[T::class] = { request -> block(request as T) }
    }

    inline fun <reified T : Notification> onNotification(): NotificationStub<T> =
        NotificationStub(T::class, notificationStubs)

    inline fun <reified T : StreamRequest<*>> onStream(): StreamStub<T> =
        StreamStub(T::class, streamStubs)

    /** Registers a [Behavior] to wrap stubbed `send` calls, in [Behavior.order]. */
    fun onPipeline(behavior: Behavior): PipelineStub =
        PipelineStub(behavior).also { pipelineStubs += it }

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
            stubs[type] = { flow<Nothing> { throw error } }
        }

        @Suppress("UNCHECKED_CAST")
        infix fun answers(block: (T) -> Flow<*>) {
            stubs[type] = { request -> block(request as T) }
        }
    }

    class PipelineStub internal constructor(private val behavior: Behavior) {
        var enabled: Boolean = behavior.isEnabled
        var order: Int = behavior.order

        internal suspend fun process(
            request: Request<*>,
            context: RequestContext,
            next: suspend () -> Any?,
        ): Any? = behavior.process(request, context, next)
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <TRequest : Request<TResult>, TResult> send(request: TRequest): TResult {
        _sent.add(request)
        val stub = requestStubs[request::class]
            ?: error("No stub registered for ${request::class.simpleName}")

        if (!pipelineEnabled) return stub(request) as TResult

        val applicable = pipelineStubs
            .filter { it.enabled }
            .sortedBy { it.order }

        if (applicable.isEmpty()) return stub(request) as TResult

        val context = RequestContext()
        val terminal: suspend () -> Any? = { stub(request) }
        val chain = applicable.foldRight(terminal) { pipeline, next ->
            suspend { pipeline.process(request, context, next) }
        }
        return chain() as TResult
    }

    override suspend fun <T : Notification> publish(notification: T) {
        _published.add(notification)
        notificationStubs[notification::class]?.invoke(notification)
    }

    override suspend fun <T : Notification> publish(notification: T, strategy: NotificationPublishStrategy) {
        _published.add(notification)
        notificationStubs[notification::class]?.invoke(notification)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <TRequest : StreamRequest<T>, T> stream(request: TRequest): Flow<T> {
        _sent.add(request as Any)
        val stub = streamStubs[request::class]
            ?: return emptyFlow()
        return stub(request) as Flow<T>
    }
}
