package com.fajrbahr.mediatork.notification

/**
 * Reacts to a specific [Notification] type.
 *
 * Multiple handlers can be registered for the same notification type; all of them
 * are invoked according to the active [NotificationPublisher] strategy. A handler
 * does not return a value — side effects only.
 *
 * @param T the notification type this handler reacts to.
 * @see NotificationPublisher
 * @see Publisher
 */
interface NotificationHandler<in T : Notification> {
    /**
     * Performs the side effect triggered by [notification].
     *
     * @param notification the notification that was published.
     */
    suspend fun handle(notification: T)
}
