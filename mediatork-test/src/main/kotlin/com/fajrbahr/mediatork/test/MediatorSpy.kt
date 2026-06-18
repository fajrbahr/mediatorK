package com.fajrbahr.mediatork.test

import com.fajrbahr.mediatork.Mediator
import com.fajrbahr.mediatork.Request
import com.fajrbahr.mediatork.StreamRequest
import com.fajrbahr.mediatork.notification.Notification
import com.fajrbahr.mediatork.notification.NotificationPublisher
import kotlinx.coroutines.flow.Flow
import kotlin.test.assertTrue

/**
 * A [Mediator] decorator that records every [send] and [publish] call, then delegates
 * to the wrapped [delegate] for actual execution.
 *
 * Use this when you want to assert *which* requests or notifications were dispatched
 * while still running real handlers — no mocking library needed.
 *
 * ```kotlin
 * val spy = MediatorSpy(FakeMediator { +CreateOrderHandler() })
 *
 * spy.send(CreateOrderCommand(id = "ORD-1"))
 *
 * spy.assertSent<CreateOrderCommand>()
 * assertEquals(1, spy.sentOf<CreateOrderCommand>().size)
 * assertEquals("ORD-1", spy.sentOf<CreateOrderCommand>().first().id)
 * ```
 *
 * @param delegate the real [Mediator] that handles requests and notifications.
 */
class MediatorSpy(private val delegate: Mediator) : Mediator {

    private val _sentRequests = Collections.synchronizedList(mutableListOf<Request<*>>())
    private val _publishedNotifications = Collections.synchronizedList(mutableListOf<Notification>())
    private val _streamedRequests = Collections.synchronizedList(mutableListOf<StreamRequest<*>>())

    /** Every request passed to [send], in dispatch order. */
    val sentRequests: List<Request<*>> get() = _sentRequests.toList()

    /** Every notification passed to [publish], in dispatch order. */
    val publishedNotifications: List<Notification> get() = _publishedNotifications.toList()

    /** Every request passed to [stream], in dispatch order. */
    val streamedRequests: List<StreamRequest<*>> get() = _streamedRequests.toList()

    override suspend fun <TRequest : Request<TResult>, TResult> send(request: TRequest): TResult {
        _sentRequests.add(request)
        return delegate.send(request)
    }

    override fun <TRequest : StreamRequest<T>, T> stream(request: TRequest): Flow<T> {
        _streamedRequests.add(request)
        return delegate.stream(request)
    }

    override suspend fun <T : Notification> publish(notification: T) {
        _publishedNotifications.add(notification)
        delegate.publish(notification)
    }

    override suspend fun <T : Notification> publish(notification: T, publisher: NotificationPublisher) {
        _publishedNotifications.add(notification)
        delegate.publish(notification, publisher)
    }

    /** Returns all sent requests of type [T]. */
    inline fun <reified T : Request<*>> sentOf(): List<T> = sentRequests.filterIsInstance<T>()

    /** Returns all published notifications of type [T]. */
    inline fun <reified T : Notification> publishedOf(): List<T> = publishedNotifications.filterIsInstance<T>()

    /**
     * Asserts that at least one request of type [T] was sent.
     *
     * @param message optional failure message prefix.
     */
    inline fun <reified T : Request<*>> assertSent(message: String? = null) {
        val prefix = if (message != null) "$message: " else ""
        assertTrue(
            sentOf<T>().isNotEmpty(),
            "${prefix}Expected at least one ${T::class.simpleName} to be sent, but none was.",
        )
    }

    /**
     * Asserts that no request of type [T] was sent.
     *
     * @param message optional failure message prefix.
     */
    inline fun <reified T : Request<*>> assertNotSent(message: String? = null) {
        val prefix = if (message != null) "$message: " else ""
        assertTrue(
            sentOf<T>().isEmpty(),
            "${prefix}Expected no ${T::class.simpleName} to be sent, but ${sentOf<T>().size} was.",
        )
    }

    /**
     * Asserts that at least one notification of type [T] was published.
     *
     * @param message optional failure message prefix.
     */
    inline fun <reified T : Notification> assertPublished(message: String? = null) {
        val prefix = if (message != null) "$message: " else ""
        assertTrue(
            publishedOf<T>().isNotEmpty(),
            "${prefix}Expected at least one ${T::class.simpleName} to be published, but none was.",
        )
    }

    /**
     * Asserts that no notification of type [T] was published.
     *
     * @param message optional failure message prefix.
     */
    inline fun <reified T : Notification> assertNotPublished(message: String? = null) {
        val prefix = if (message != null) "$message: " else ""
        assertTrue(
            publishedOf<T>().isEmpty(),
            "${prefix}Expected no ${T::class.simpleName} to be published, but ${publishedOf<T>().size} was.",
        )
    }

    /**
     * Asserts that exactly [count] requests of type [T] were sent.
     */
    inline fun <reified T : Request<*>> assertSentCount(count: Int, message: String? = null) {
        val actual = sentOf<T>().size
        val prefix = if (message != null) "$message: " else ""
        assertTrue(
            actual == count,
            "${prefix}Expected $count ${T::class.simpleName} to be sent, but was $actual.",
        )
    }

    /**
     * Asserts that exactly [count] notifications of type [T] were published.
     */
    inline fun <reified T : Notification> assertPublishedCount(count: Int, message: String? = null) {
        val actual = publishedOf<T>().size
        val prefix = if (message != null) "$message: " else ""
        assertTrue(
            actual == count,
            "${prefix}Expected $count ${T::class.simpleName} to be published, but was $actual.",
        )
    }

    /** Returns all streamed requests of type [T]. */
    inline fun <reified T : StreamRequest<*>> streamedOf(): List<T> = streamedRequests.filterIsInstance<T>()

    /** Asserts that at least one stream request of type [T] was dispatched. */
    inline fun <reified T : StreamRequest<*>> assertStreamed(message: String? = null) {
        val prefix = if (message != null) "$message: " else ""
        assertTrue(
            streamedOf<T>().isNotEmpty(),
            "${prefix}Expected at least one ${T::class.simpleName} to be streamed, but none was.",
        )
    }

    /** Asserts that no stream request of type [T] was dispatched. */
    inline fun <reified T : StreamRequest<*>> assertNotStreamed(message: String? = null) {
        val prefix = if (message != null) "$message: " else ""
        assertTrue(
            streamedOf<T>().isEmpty(),
            "${prefix}Expected no ${T::class.simpleName} to be streamed, but ${streamedOf<T>().size} was.",
        )
    }

    /** Asserts that exactly [count] stream requests of type [T] were dispatched. */
    inline fun <reified T : StreamRequest<*>> assertStreamedCount(count: Int, message: String? = null) {
        val actual = streamedOf<T>().size
        val prefix = if (message != null) "$message: " else ""
        assertTrue(
            actual == count,
            "${prefix}Expected $count ${T::class.simpleName} to be streamed, but was $actual.",
        )
    }

    /** Clears all recorded sends, publishes, and streams. */
    fun reset() {
        _sentRequests.clear()
        _publishedNotifications.clear()
        _streamedRequests.clear()
    }
}

// Alias so it doesn't conflict with kotlin.collections.Collections
private typealias Collections = java.util.Collections
