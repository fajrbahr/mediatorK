package com.fajrbahr.mediatork

interface NotificationPublisher {
    suspend fun <T : Notification> publish(notification: T, handlers: List<NotificationHandler<T>>)
}
