package com.fajrbahr.mediatork.notification

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * [NotificationPublisher] that dispatches handlers on a caller-supplied [CoroutineScope]
 * and returns immediately without waiting for them to complete.
 *
 * Because handlers run outside the calling coroutine's structured-concurrency scope,
 * failures are not propagated to the caller — they surface only via the exception
 * handler of the provided [scope]. Use this strategy for truly non-critical side
 * effects such as analytics events or cache warm-ups where the caller must not be
 * blocked or made to handle failures.
 *
 * The [scope] must outlive the expected handler execution time. Cancelling [scope]
 * before handlers complete will cancel those handlers.
 *
 * @param scope the coroutine scope in which handlers are launched.
 * @see ParallelNotificationPublisher
 * @see SequentialNotificationPublisher
 */
class FireAndForgetNotificationPublisher(
    private val scope: CoroutineScope,
) : NotificationPublisher {
    /**
     * Launches each handler in [scope] and returns without waiting.
     *
     * @param T the concrete notification type.
     * @param notification the notification to deliver.
     * @param handlers the list of handlers to fire.
     */
    override suspend fun <T : Notification> publish(notification: T, handlers: List<NotificationHandler<T>>) {
        handlers.forEach { handler ->
            scope.launch { handler.handle(notification) }
        }
    }
}
