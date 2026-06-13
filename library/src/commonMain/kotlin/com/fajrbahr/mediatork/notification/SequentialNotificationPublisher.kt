package com.fajrbahr.mediatork.notification

/**
 * [NotificationPublisher] that invokes handlers one at a time, in registration order.
 *
 * Execution stops at the first handler that throws — subsequent handlers are not
 * called and the exception propagates to the caller. Use this strategy when handler
 * ordering matters or when the outcome of one handler should gate the next.
 *
 * @see ParallelNotificationPublisher
 * @see ContinueOnExceptionNotificationPublisher
 */
class SequentialNotificationPublisher : NotificationPublisher {
    /**
     * Invokes each handler in [handlers] sequentially. Stops and rethrows on first failure.
     *
     * @param T the concrete notification type.
     * @param notification the notification to deliver.
     * @param handlers the list of handlers to invoke in order.
     */
    override suspend fun <T : Notification> publish(notification: T, handlers: List<NotificationHandler<T>>) {
        handlers.forEach { it.handle(notification) }
    }
}
