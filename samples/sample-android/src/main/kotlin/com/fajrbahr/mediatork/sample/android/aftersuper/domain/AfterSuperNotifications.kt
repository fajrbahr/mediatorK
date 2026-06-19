package com.fajrbahr.mediatork.sample.android.aftersuper.domain

import android.util.Log
import com.fajrbahr.mediatork.notification.Notification
import com.fajrbahr.mediatork.api.NotificationHandler

data class PrayerTimesFetchedNotification(
    val city: String,
    val source: String, // "network" or "cache"
) : Notification

class LogPrayerTimesFetchedHandler : NotificationHandler<PrayerTimesFetchedNotification> {
    override suspend fun handle(notification: PrayerTimesFetchedNotification) {
        Log.d("MediatorK", "[Notification] Prayer times for '${notification.city}' loaded from ${notification.source}")
    }
}

// Demonstrates FallbackNotificationHandler via `otherwise` — fires when primary throws
class AnalyticsPrayerTimesFetchedHandler : NotificationHandler<PrayerTimesFetchedNotification> {
    override suspend fun handle(notification: PrayerTimesFetchedNotification) {
        Log.d("MediatorK", "[Analytics] Tracked fetch — city='${notification.city}' source=${notification.source}")
    }
}
