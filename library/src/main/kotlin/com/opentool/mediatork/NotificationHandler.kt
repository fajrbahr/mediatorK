package com.opentool.mediatork.com.opentool.mediatork

interface NotificationHandler<in T : Notification> {
    suspend fun handle(notification: T)
}
