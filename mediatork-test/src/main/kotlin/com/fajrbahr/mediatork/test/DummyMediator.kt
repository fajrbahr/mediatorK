package com.fajrbahr.mediatork.test

import com.fajrbahr.mediatork.*

/**
 * A no-op [Mediator] for tests that don't need any handler behaviour.
 *
 * [send] silently returns without doing anything (result is `Unit` cast to `TResult`).
 * [publish] does nothing. Use this when you only need a [Mediator] instance to satisfy a
 * constructor and never actually send requests in the test.
 *
 * ```kotlin
 * val vm = OrderViewModel(DummyMediator())
 * ```
 */
class DummyMediator : Mediator {
    @Suppress("UNCHECKED_CAST")
    override suspend fun <TRequest : Request<TResult>, TResult> send(request: TRequest): TResult =
        Unit as TResult

    override suspend fun <T : Notification> publish(notification: T) = Unit
    override suspend fun <T : Notification> publish(notification: T, publisher: NotificationPublisher) = Unit
}
