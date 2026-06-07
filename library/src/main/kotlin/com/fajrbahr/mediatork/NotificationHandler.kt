package com.fajrbahr.mediatork

interface NotificationHandler<in T : Notification> {
    suspend fun handle(notification: T)
}
