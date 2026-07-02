package com.fajrbahr.mediatork.test

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.MediatorFactory
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.MediatorRegistrar
import com.fajrbahr.mediatork.api.Notification
import com.fajrbahr.mediatork.api.NotificationHandler
import com.fajrbahr.mediatork.api.PipelineBehavior
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.api.StreamRequest
import com.fajrbahr.mediatork.api.StreamRequestHandler
import com.fajrbahr.mediatork.notification.NotificationPublishStrategy
import com.fajrbahr.mediatork.notification.ParallelNotificationPublisher
import kotlinx.coroutines.flow.Flow

/**
 * A test-only [com.fajrbahr.mediatork.api.Mediator] backed by a real [HandlerRegistry] and [MediatorFactory].
 *
 * Handlers can be registered at construction time via the [init] block or the
 * [registrars] list, and also added at any time after construction by calling
 * [register]. Because the underlying [MediatorFactory.create] overload that
 * accepts an existing registry is used, late-registered handlers are picked up
 * immediately on the next [send] call.
 *
 * ```kotlin
 * val mediator = FakeMediator {
 *     +CreateOrderHandler()
 * }
 *
 * // or register later:
 * mediator.register<CreateOrderCommand, OrderResult>(CreateOrderHandler())
 * ```
 */
class FakeMediator(
    registrars: List<MediatorRegistrar> = emptyList(),
    pipelineBehaviors: List<PipelineBehavior> = emptyList(),
    notificationPublisher: NotificationPublishStrategy = ParallelNotificationPublisher(),
    init: HandlerRegistry.() -> Unit = {},
) : Mediator {

    val registry = HandlerRegistry().apply {
        registrars.forEach { it.register(this) }
        init()
    }

    private val mediator = MediatorFactory.create(
        registry = registry,
        pipelineBehaviors = pipelineBehaviors,
        notificationPublisher = notificationPublisher,
    )

    inline fun <reified TRequest : Request<TResult>, TResult> register(
        handler: RequestHandler<TRequest, TResult>,
    ) = registry.register(handler)

    inline fun <reified TRequest : StreamRequest<T>, T> registerStream(
        handler: StreamRequestHandler<TRequest, T>,
    ) = registry.registerStream(handler)

    override suspend fun <TRequest : Request<TResult>, TResult> send(request: TRequest): TResult =
        mediator.send(request)

    override fun <TRequest : StreamRequest<T>, T> stream(request: TRequest): Flow<T> =
        mediator.stream(request)

    override suspend fun <T : Notification> publish(notification: T) =
        mediator.publish(notification)

    override suspend fun <T : Notification> publish(notification: T, publisher: NotificationPublishStrategy) =
        mediator.publish(notification, publisher)
}

/**
 * Creates a [RequestHandler] from a suspend lambda, inferring the request and result types.
 *
 * ```kotlin
 * val handler = fakeHandler<CreateOrderCommand, OrderResult> { mediator, ctx, request ->
 *     OrderResult(orderId = request.id)
 * }
 * mediator.register(handler)
 * ```
 */
inline fun <reified TRequest : Request<TResult>, TResult> fakeHandler(
    noinline handler: suspend (mediator: Mediator, RequestContext, TRequest) -> TResult,
): RequestHandler<TRequest, TResult> = object : RequestHandler<TRequest, TResult> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: TRequest,
    ): TResult = handler(mediator, requestContext, request)
}

/**
 * Creates a [NotificationHandler] from a suspend lambda.
 *
 * ```kotlin
 * val handler = fakeNotificationHandler<OrderPlacedEvent> { notification ->
 *     capturedEvents += notification
 * }
 * mediator.registry.registerNotification(handler)
 * ```
 */
inline fun <reified T : Notification> fakeNotificationHandler(
    noinline handler: suspend (T) -> Unit,
): NotificationHandler<T> = object : NotificationHandler<T> {
    override suspend fun handle(notification: T) = handler(notification)
}

/**
 * Registers a [NotificationHandler] for [T] and returns the live list that collects every
 * notification published to this mediator.
 *
 * ```kotlin
 * val events = mediator.captureNotifications<OrderPlacedEvent>()
 * mediator.publish(OrderPlacedEvent(orderId = "ORD-1"))
 * assertEquals("ORD-1", events.first().orderId)
 * ```
 */
inline fun <reified T : Notification> FakeMediator.captureNotifications(): List<T> {
    val captured = mutableListOf<T>()
    registry.registerNotification(fakeNotificationHandler<T> { captured += it })
    return captured
}

