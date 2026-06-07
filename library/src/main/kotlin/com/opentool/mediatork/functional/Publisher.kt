package com.opentool.mediatork.com.opentool.mediatork.functional

import com.opentool.mediatork.com.opentool.mediatork.functional.Notification
import com.opentool.mediatork.com.opentool.mediatork.functional.NotificationPublisher

interface Publisher {
    suspend fun <T : Notification> publish(notification: T)
    suspend fun <T : Notification> publish(notification: T, publisher: NotificationPublisher)
}
