package com.fajrbahr.mediatork.notification

import com.fajrbahr.mediatork.api.Notification

/**
 * Capability for broadcasting a [com.fajrbahr.mediatork.api.Notification] to all registered handlers.
 *
 * Unlike [com.fajrbahr.mediatork.handler.Sender], publishing returns no value. When no handlers are registered for
 * the notification type, the configured missing-notification handler is invoked —
 * by default [ThrowMissingNotificationHandler], which throws
 * [com.fajrbahr.mediatork.MissingNotificationHandlerException].
 * The delivery strategy (parallel, sequential, fire-and-forget, etc.) is determined
 * by the active [NotificationPublishStrategy].
 *
 * @see com.fajrbahr.mediatork.api.Mediator
 * @see com.fajrbahr.mediatork.api.NotificationHandler
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

    /**
     * Broadcasts [notification] to all registered handlers without compile-time type
     * information.
     *
     * Use this overload only in dynamic/reflection-based scenarios where the concrete
     * notification type is not known at compile time. Prefer the typed [publish] overload
     * everywhere else.
     *
     * [notification] must implement [Notification]; otherwise [IllegalArgumentException] is thrown.
     *
     * @param notification the notification to broadcast; must implement [Notification].
     * @throws IllegalArgumentException if [notification] does not implement [Notification].
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun publishDynamic(notification: Any) {
        require(notification is Notification) { "publishDynamic: $notification does not implement Notification" }
        publish(notification)
    }

}
