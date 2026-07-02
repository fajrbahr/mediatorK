package com.fajrbahr.mediatork.api

/**
 * Reacts to a specific [Notification] type.
 *
 * Multiple handlers can be registered for the same notification type; all of them
 * are invoked according to the active
 * [com.fajrbahr.mediatork.notification.NotificationPublishStrategy]. A handler
 * does not return a value — side effects only.
 *
 * @param T the notification type this handler reacts to.
 * @see com.fajrbahr.mediatork.notification.NotificationPublishStrategy
 * @see com.fajrbahr.mediatork.notification.Publisher
 */
interface NotificationHandler<in T : Notification> {

    /**
     * Relative execution order among all handlers registered for the same notification type.
     *
     * Lower values run first. When two handlers share the same [order], they run in
     * registration order — the sort is stable. Defaults to `0`.
     *
     * Ordering is applied before the active
     * [com.fajrbahr.mediatork.notification.NotificationPublishStrategy], so it affects
     * both sequential and parallel strategies (parallel strategies receive handlers
     * pre-sorted but may launch them concurrently).
     */
    val order: Int get() = 0

    /**
     * Performs the side effect triggered by [notification].
     *
     * @param notification the notification that was published.
     */
    suspend fun handle(notification: T)
}
