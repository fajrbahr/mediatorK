package com.fajrbahr.mediatork.sample.ktor.aftersuper.domain

import com.fajrbahr.mediatork.notification.Notification
import com.fajrbahr.mediatork.notification.NotificationHandler

data class PrayerTimesFetchedNotification(
    val city: String,
    val source: String, // "network" or "cache"
) : Notification

class LogPrayerTimesFetchedHandler : NotificationHandler<PrayerTimesFetchedNotification> {
    override suspend fun handle(notification: PrayerTimesFetchedNotification) {
        println("[Notification] Prayer times for '${notification.city}' loaded from ${notification.source}")
    }
}

// Demonstrates FallbackNotificationHandler via `otherwise` — fires when primary throws
class AnalyticsPrayerTimesFetchedHandler : NotificationHandler<PrayerTimesFetchedNotification> {
    override suspend fun handle(notification: PrayerTimesFetchedNotification) {
        println("[Analytics] Tracked fetch — city='${notification.city}' source=${notification.source}")
    }
}
