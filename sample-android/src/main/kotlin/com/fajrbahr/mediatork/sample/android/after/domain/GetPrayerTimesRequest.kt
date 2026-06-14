package com.fajrbahr.mediatork.sample.android.after.domain

import com.fajrbahr.mediatork.Request
import com.fajrbahr.mediatork.sample.android.after.model.TodayPrayerTimes

data class GetPrayerTimesRequest(
    val latitude: Double = 51.5194682,
    val longitude: Double = -0.1360365,
    val method: Int = 3,
) : Request<TodayPrayerTimes>
