package com.fajrbahr.mediatork.test

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.MediatorBuilder
import com.fajrbahr.mediatork.MediatorFactory
import com.fajrbahr.mediatork.MediatorModule
import com.fajrbahr.mediatork.api.*
import com.fajrbahr.mediatork.feature.Feature
import com.fajrbahr.mediatork.feature.StreamFeature
import com.fajrbahr.mediatork.notification.NotificationPublishStrategy
import kotlinx.coroutines.flow.Flow

/**
 * A test-only [com.fajrbahr.mediatork.api.Mediator] backed by a real [HandlerRegistry] and [MediatorFactory].
 *
 * Handlers can be registered at construction time via the [init] block,
 * and also added at any time after construction by calling
 * [register].
 *
 * ```kotlin
 * val mediator = FakeMediator {
 *     handle<CreateOrderCommand, OrderResult> { ... }
 * }
 *
 * // or register later:
 * mediator.register<CreateOrderCommand, OrderResult>(CreateOrderHandler())
 * ```
 */
class FakeMediator(
    pipelineBehaviors: List<PipelineBehavior> = emptyList(),
    streamPipelineBehaviors: List<StreamPipelineBehavior> = emptyList(),
    notificationPublisher: NotificationPublishStrategy = NotificationPublishStrategy.ParallelNotificationPublisher(),
    init: MediatorModule = {},
) : Mediator {

    val builder = MediatorBuilder().apply {
        this.add(*pipelineBehaviors.toTypedArray())
        this.add(*streamPipelineBehaviors.toTypedArray())
        this.notificationPublisher = notificationPublisher
        this.verifyHandlers = false
        init()
    }

    val registry: HandlerRegistry get() = builder.registry

    private val mediator = builder.build()

    inline fun <reified TRequest : Request<TResult>, TResult> register(
        handler: RequestHandler<TRequest, TResult>,
    ) = registry.register(handler)

    inline fun <reified TRequest : StreamRequest<T>, T> registerStream(
        handler: StreamRequestHandler<TRequest, T>,
    ) = registry.registerStream(handler)

    inline fun <reified TRequest : Request<TResult>, TResult> register(
        feature: Feature<TRequest, TResult>,
    ) = registry.registerFeature(feature)

    inline fun <reified TRequest : StreamRequest<T>, T> register(
        feature: StreamFeature<TRequest, T>,
    ) = registry.registerStreamFeature(feature)

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
 * // Use captureNotifications() or register via the FakeMediator directly
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
