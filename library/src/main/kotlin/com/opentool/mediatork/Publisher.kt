package com.opentool.mediatork.com.opentool.mediatork

interface Publisher {
    suspend fun <T : Notification> publish(notification: T)
    suspend fun <T : Notification> publish(notification: T, publisher: NotificationPublisher)
}
