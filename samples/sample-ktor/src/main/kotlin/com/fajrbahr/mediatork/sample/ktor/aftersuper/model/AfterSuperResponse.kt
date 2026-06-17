package com.fajrbahr.mediatork.sample.ktor.aftersuper.model

import com.fajrbahr.mediatork.sample.ktor.after.model.IslamicMonth
import com.fajrbahr.mediatork.sample.ktor.after.model.TodayPrayerTimes
import kotlinx.serialization.Serializable

@Serializable
data class AfterSuperPrayerTimesResponse(
    val prayerTimes: TodayPrayerTimes,
    val requestCount: Long,
)

@Serializable
data class AfterSuperIslamicMonthsResponse(
    val months: List<IslamicMonth>,
    val requestCount: Long,
)
