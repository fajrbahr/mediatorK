package com.opentool.mediatork.com.opentool.mediatork.functional

interface NotificationPublisher {
    suspend fun <T : Notification> publish(notification: T, handlers: List<NotificationHandler<T>>)
}
