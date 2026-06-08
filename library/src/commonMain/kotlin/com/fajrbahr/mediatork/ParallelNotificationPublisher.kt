package com.fajrbahr.mediatork

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

/**
 * [NotificationPublisher] that invokes all handlers concurrently and waits for
 * every one to complete before returning.
 *
 * All handlers are launched as child coroutines within a [coroutineScope], so
 * structured concurrency guarantees apply: if any handler throws, the scope
 * cancels remaining handlers and the exception propagates to the caller.
 *
 * This is the default publisher used by [MediatorFactory.create].
 *
 * @see SequentialNotificationPublisher
 * @see ContinueOnExceptionNotificationPublisher
 * @see FireAndForgetNotificationPublisher
 */
class ParallelNotificationPublisher : NotificationPublisher {
    /**
     * Launches each handler in [handlers] as a concurrent coroutine and waits
     * for all of them to finish.
     *
     * @param T the concrete notification type.
     * @param notification the notification to deliver to all handlers.
     * @param handlers the list of handlers to invoke in parallel.
     */
    override suspend fun <T : Notification> publish(notification: T, handlers: List<NotificationHandler<T>>) {
        coroutineScope {
            handlers.map { handler -> launch { handler.handle(notification) } }.joinAll()
        }
    }
}
