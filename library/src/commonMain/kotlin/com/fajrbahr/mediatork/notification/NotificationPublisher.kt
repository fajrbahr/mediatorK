package com.fajrbahr.mediatork.notification

/**
 * Strategy that controls how a [Notification] is delivered to its handlers.
 *
 * Built-in implementations:
 * - [ParallelNotificationPublisher] — all handlers run concurrently (default).
 * - [SequentialNotificationPublisher] — handlers run one-by-one; first failure aborts.
 * - [ContinueOnExceptionNotificationPublisher] — all handlers run even if some fail;
 *   failures are collected and rethrown as [AggregateException].
 * - [FireAndForgetNotificationPublisher] — returns immediately; handlers run in the
 *   background on a caller-supplied [kotlinx.coroutines.CoroutineScope].
 *
 * Custom implementations can enforce ordering, add tracing, or apply retry logic.
 *
 * @see Publisher
 */
interface NotificationPublisher {
    /**
     * Delivers [notification] to every handler in [handlers] according to this strategy.
     *
     * @param T the concrete notification type.
     * @param notification the notification to deliver.
     * @param handlers the pre-resolved list of handlers to invoke.
     */
    suspend fun <T : Notification> publish(notification: T, handlers: List<NotificationHandler<T>>)
}
