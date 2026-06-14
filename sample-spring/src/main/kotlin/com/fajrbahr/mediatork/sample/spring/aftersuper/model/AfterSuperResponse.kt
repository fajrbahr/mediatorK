package com.fajrbahr.mediatork.sample.spring.aftersuper.model

import com.fajrbahr.mediatork.sample.spring.after.model.IslamicMonth
import com.fajrbahr.mediatork.sample.spring.after.model.TodayPrayerTimes

data class AfterSuperPrayerTimesResponse(
    val prayerTimes: TodayPrayerTimes,
    val requestCount: Long,
)

data class AfterSuperIslamicMonthsResponse(
    val months: List<IslamicMonth>,
    val requestCount: Long,
)
