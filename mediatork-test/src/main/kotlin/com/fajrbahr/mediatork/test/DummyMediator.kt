package com.fajrbahr.mediatork.test

import com.fajrbahr.mediatork.Mediator
import com.fajrbahr.mediatork.Request
import com.fajrbahr.mediatork.StreamRequest
import com.fajrbahr.mediatork.notification.Notification
import com.fajrbahr.mediatork.notification.NotificationPublisher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * A no-op [Mediator] for tests that don't need any handler behaviour.
 *
 * [send] silently returns, [publish] does nothing. Use this when you only need a [Mediator]
 * to satisfy a constructor and never actually call [send] in the test.
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
    override fun <TRequest : StreamRequest<T>, T> stream(request: TRequest): Flow<T> = emptyFlow()
}
