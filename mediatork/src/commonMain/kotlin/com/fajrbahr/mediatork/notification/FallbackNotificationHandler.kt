@file:Suppress("TooGenericExceptionCaught")

package com.fajrbahr.mediatork.notification

import com.fajrbahr.mediatork.api.Notification
import com.fajrbahr.mediatork.api.NotificationHandler

/**
 * Tries each handler in [handlers] in order, stopping at the first success.
 * If a handler throws, the exception is swallowed and the next handler is tried.
 * Re-throws the last handler's exception if every handler fails.
 *
 * Compose with [orElse] instead of constructing directly.
 */
internal class FallbackNotificationHandler<T : Notification>(
    private val handlers: List<NotificationHandler<T>>,
) : NotificationHandler<T> {

    override suspend fun handle(notification: T) {
        var lastException: Throwable? = null
        for (handler in handlers) {
            try {
                handler.handle(notification)
                return
            } catch (e: Throwable) {
                lastException = e
            }
        }
        throw lastException ?: error("FallbackNotificationHandler has no handlers")
    }

    internal fun withFallback(handler: NotificationHandler<T>): FallbackNotificationHandler<T> =
        FallbackNotificationHandler(handlers + handler)
}

/**
 * Returns a handler that tries `this` first, then [fallback] if `this` throws.
 *
 * Chains naturally: `a otherwise b otherwise c` produces a single [FallbackNotificationHandler]
 * with three candidates tried in order.
 */
infix fun <T : Notification> NotificationHandler<T>.orElse(
    fallback: NotificationHandler<T>,
): NotificationHandler<T> = when (this) {
    is FallbackNotificationHandler -> withFallback(fallback)
    else -> FallbackNotificationHandler(listOf(this, fallback))
}
