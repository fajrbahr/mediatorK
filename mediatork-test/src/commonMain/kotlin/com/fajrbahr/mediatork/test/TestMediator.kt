package com.fajrbahr.mediatork.test

import com.fajrbahr.mediatork.MediatorBuilder
import com.fajrbahr.mediatork.mediatorK
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Notification
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.StreamRequest
import com.fajrbahr.mediatork.notification.NotificationPublishStrategy
import kotlinx.coroutines.flow.Flow

/**
 * A real [Mediator] for tests — no mocks, no stubs.
 *
 * Register handlers with the exact same DSL you use in production ([MediatorBuilder.handle],
 * [MediatorBuilder.handleStream], [MediatorBuilder.notification], [MediatorBuilder.validate],
 * [MediatorBuilder.behaviors]). The returned mediator runs the real pipeline and *additionally*
 * records every request and notification so you can assert on them.
 *
 * Testing a request handler is just "pass a handler body that returns a value":
 *
 * ```kotlin
 * val mediator = testMediator {
 *     handle<GetUserQuery, String> { "user:${it.id}" }
 * }
 * assertEquals("user:42", mediator.send(GetUserQuery("42")))
 * assertEquals(1, mediator.sentOf<GetUserQuery>().size)
 * ```
 *
 * Dynamic responses, errors, notifications and behaviors are all just the real DSL:
 *
 * ```kotlin
 * val received = mutableListOf<OrderPlacedEvent>()
 * val mediator = testMediator {
 *     handle<CreateOrderCommand, String> { if (it.id.isBlank()) error("bad id") else "order:${it.id}" }
 *     notification<OrderPlacedEvent> { received += it }
 *     validate<CreateOrderCommand> { rules { check(it.id.isNotBlank()) { "id required" } } }
 *     behaviors(logging())
 * }
 * ```
 */
fun testMediator(block: MediatorBuilder.() -> Unit = {}): RecordingMediator =
    RecordingMediator(mediatorK(block))

/**
 * Wraps a real [Mediator], delegating every call to it while recording what passed through.
 * Prefer the [testMediator] factory; construct this directly only to record around a mediator
 * you already built (e.g. an assembled set of production modules).
 *
 * ```kotlin
 * val mediator = RecordingMediator(mediatorK { courseModule(store) })
 * ```
 */
class RecordingMediator(private val delegate: Mediator) : Mediator {

    @PublishedApi
    internal val _sent = mutableListOf<Any>()

    /** Every request passed to [send] and [stream], in call order. */
    val sent: List<Any> get() = _sent.toList()

    @PublishedApi
    internal val _published = mutableListOf<Notification>()

    /** Every notification passed to [publish], in call order. */
    val published: List<Notification> get() = _published.toList()

    /** The recorded requests of type [T], in call order. */
    inline fun <reified T> sentOf(): List<T> = _sent.filterIsInstance<T>()

    /** The recorded notifications of type [T], in call order. */
    inline fun <reified T : Notification> publishedOf(): List<T> = _published.filterIsInstance<T>()

    override suspend fun <TRequest : Request<TResult>, TResult> send(request: TRequest): TResult {
        _sent.add(request)
        return delegate.send(request)
    }

    override fun <TRequest : StreamRequest<T>, T> stream(request: TRequest): Flow<T> {
        _sent.add(request)
        return delegate.stream(request)
    }

    override suspend fun <T : Notification> publish(notification: T) {
        _published.add(notification)
        delegate.publish(notification)
    }

    override suspend fun <T : Notification> publish(notification: T, strategy: NotificationPublishStrategy) {
        _published.add(notification)
        delegate.publish(notification, strategy)
    }
}
