package com.opentool.mediatork.com.opentool.mediatork.functional

val SequentialNotificationPublisher: NotificationPublisher = object : NotificationPublisher {
    override suspend fun <T : Notification> publish(notification: T, handlers: List<NotificationHandler<T>>) {
        handlers.forEach { it(notification) }
    }
}
