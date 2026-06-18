package com.fajrbahr.mediatork.notification

/**
 * Capability for broadcasting a [Notification] to all registered handlers.
 *
 * Unlike [com.fajrbahr.mediatork.Sender], publishing returns no value. When no handlers are registered for
 * the notification type, the configured missing-notification handler is invoked —
 * by default [ThrowMissingNotificationHandler], which throws [com.fajrbahr.mediatork.MissingNotificationHandlerException].
 * The delivery strategy (parallel, sequential, fire-and-forget, etc.) is determined
 * by the active [NotificationPublishStrategy].
 *
 * @see com.fajrbahr.mediatork.Mediator
 * @see NotificationHandler
 * @see NotificationPublishStrategy
 */
interface Publisher {
    /**
     * Broadcasts [notification] to all registered handlers using the default
     * [NotificationPublishStrategy] configured at mediator creation time.
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
    suspend fun <T : Notification> publish(notification: T, publisher: NotificationPublishStrategy)
}
