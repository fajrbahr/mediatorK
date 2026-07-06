package com.fajrbahr.mediatork.test

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.MediatorFactory
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Notification
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.StreamRequest
import com.fajrbahr.mediatork.notification.NotificationPublishStrategy
import kotlinx.coroutines.flow.Flow

/**
 * A [Mediator] decorator that routes overridden request/notification types to a
 * local [HandlerRegistry] and delegates everything else to the production [base].
 *
 * Use this when you already have a fully configured production mediator and want
 * to test against it — swapping only the handlers or notifications under test:
 *
 * ```kotlin
 * val testMediator = productionMediator.forTesting {
 *     handle<GetPriceQuery, FormattedPrice> { request ->
 *         FormattedPrice("$0.00")
 *     }
 *     on<OrderCreatedNotification> { captured += it }
 * }
 * val harness = HandlerTestHarness(testMediator)
 * harness.send(GetPriceQuery("PROD-1"))   // → override
 * harness.send(GetOrderQuery("ORD-1"))    // → production pipeline
 * ```
 *
 * Non-overridden requests go through the full production pipeline (behaviors,
 * validators, etc.) unchanged. Overridden handlers run through a minimal pipeline
 * (validators from the override registry only, no production behaviors).
 *
 * @param base the production [Mediator] to delegate non-overridden calls to.
 * @param init DSL block for registering override handlers on a [HandlerRegistry].
 */
class TestMediator(
    private val base: Mediator,
    init: HandlerRegistry.() -> Unit = {},
) : Mediator {

    val registry: HandlerRegistry = HandlerRegistry().apply(init)

    private val overrideMediator: Mediator = MediatorFactory.create(registry = registry)

    override suspend fun <TRequest : Request<TResult>, TResult> send(request: TRequest): TResult =
        if (registry.hasHandler(request::class)) overrideMediator.send(request)
        else base.send(request)

    override fun <TRequest : StreamRequest<T>, T> stream(request: TRequest): Flow<T> =
        if (registry.hasStreamHandler(request::class)) overrideMediator.stream(request)
        else base.stream(request)

    override suspend fun <T : Notification> publish(notification: T) {
        if (registry.hasNotificationHandler(notification::class))
            overrideMediator.publish(notification)
        else
            base.publish(notification)
    }

    override suspend fun <T : Notification> publish(notification: T, publisher: NotificationPublishStrategy) {
        if (registry.hasNotificationHandler(notification::class))
            overrideMediator.publish(notification, publisher)
        else
            base.publish(notification, publisher)
    }
}

/**
 * Wraps this [Mediator] for testing, routing overridden handlers to a local registry
 * while delegating everything else to the full production pipeline.
 *
 * ```kotlin
 * val testMediator = productionMediator.forTesting {
 *     handle<GetPriceQuery, FormattedPrice> { FormattedPrice("$0.00") }
 *     on<OrderCreatedNotification> { captured += it }
 * }
 * ```
 */
fun Mediator.forTesting(
    overrides: HandlerRegistry.() -> Unit = {},
): TestMediator = TestMediator(this, overrides)
