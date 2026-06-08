package com.fajrbahr.mediatork

/**
 * Capability for broadcasting a [Notification] to all registered handlers.
 *
 * Unlike [Sender], publishing never returns a value and never throws if there are
 * zero handlers registered for the notification type. The delivery strategy
 * (parallel, sequential, fire-and-forget, etc.) is determined by the active
 * [NotificationPublisher].
 *
 * @see Mediator
 * @see NotificationHandler
 * @see NotificationPublisher
 */
interface Publisher {
    /**
     * Broadcasts [notification] to all registered handlers using the default
     * [NotificationPublisher] configured at mediator creation time.
     *
     * @param T the concrete notification type.
     * @param notification the notification to broadcast.
     */
    suspend fun <T : Notification> publish(notification: T)

    /**
     * Broadcasts [notification] to all registered handlers using the given [publisher],
     * overriding the default publisher for this call only.
     *
     * Use this overload when a specific dispatch strategy is needed for a single
     * publish site — for example, using [SequentialNotificationPublisher] for an
     * ordered workflow while the default remains [ParallelNotificationPublisher].
     *
     * @param T the concrete notification type.
     * @param notification the notification to broadcast.
     * @param publisher the strategy to use for this publish call.
     */
    suspend fun <T : Notification> publish(notification: T, publisher: NotificationPublisher)
}
