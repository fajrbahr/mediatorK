package com.fajrbahr.mediatork.notification

import com.fajrbahr.mediatork.AggregateException

/**
 * [NotificationPublisher] that runs every handler regardless of failures and then
 * reports all errors together.
 *
 * Each handler is invoked in sequence. Any exception thrown is caught and collected.
 * After all handlers have been attempted, if at least one failed, a [com.fajrbahr.mediatork.AggregateException]
 * containing every collected error is thrown. This guarantees that a single misbehaving
 * handler cannot silently prevent other handlers from executing.
 *
 * @see ParallelNotificationPublisher
 * @see SequentialNotificationPublisher
 * @see com.fajrbahr.mediatork.AggregateException
 */
class ContinueOnExceptionNotificationPublisher : NotificationPublisher {
    /**
     * Invokes each handler in [handlers] in sequence, collecting any thrown exceptions.
     * Throws [AggregateException] if one or more handlers failed.
     *
     * @param T the concrete notification type.
     * @param notification the notification to deliver to all handlers.
     * @param handlers the list of handlers to invoke.
     * @throws com.fajrbahr.mediatork.AggregateException if any handler threw an exception.
     */
    override suspend fun <T : Notification> publish(
        notification: T,
        handlers: List<NotificationHandler<T>>
    ) {
        val errors = mutableListOf<Throwable>()
        handlers.forEach { handler ->
            try {
                handler.handle(notification)
            } catch (e: Throwable) {
                errors.add(e)
            }
        }
        if (errors.isNotEmpty()) throw AggregateException(errors)
    }
}
