package com.opentool.mediatork.com.opentool.mediatork


class SequentialNotificationPublisher : NotificationPublisher {
    override suspend fun <T : Notification> publish(notification: T, handlers: List<NotificationHandler<T>>) {
        handlers.forEach { it.handle(notification) }
    }
}
