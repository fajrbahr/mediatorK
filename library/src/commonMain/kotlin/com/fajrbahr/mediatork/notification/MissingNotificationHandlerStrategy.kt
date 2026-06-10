package com.fajrbahr.mediatork.notification

/**
 * Throws [MissingNotificationHandlerException] when a notification is published
 * with no registered handlers.
 *
 * This is the default [MissingNotificationHandler] passed to [MediatorFactory.create].
 * It surfaces misconfiguration immediately rather than silently dropping notifications.
 */
class ThrowMissingNotificationHandler : NotificationHandler<Notification> {
    override suspend fun handle(notification: Notification) {
        throw MissingNotificationHandlerException(
            notificationTypeName = notification::class.simpleName ?: "Unknown"
        )
    }
}

/**
 * Does nothing when a notification is published with no registered handlers.
 *
 * Only use this when unhandled notifications are intentional. Misconfiguration
 * will produce no error and no trace — silent data loss.
 */
class SilentMissingNotificationHandler : NotificationHandler<Notification> {
    override suspend fun handle(notification: Notification) = Unit
}
