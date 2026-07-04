package com.fajrbahr.mediatork.api

/**
 * Handles a specific [Notification] type.
 */
fun interface NotificationHandler<in T : Notification> {
    val order: Int get() = 0
    suspend fun handle(notification: T)
}
