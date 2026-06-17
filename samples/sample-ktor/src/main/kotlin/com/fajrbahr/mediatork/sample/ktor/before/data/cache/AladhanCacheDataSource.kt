package com.fajrbahr.mediatork.sample.ktor.before.data.cache

import com.fajrbahr.mediatork.sample.ktor.before.model.IslamicMonth
import com.fajrbahr.mediatork.sample.ktor.before.model.TodayPrayerTimes
import java.time.LocalDate

class AladhanCacheDataSource {
    private val prayerTimesCache = HashMap<String, TodayPrayerTimes>()
    private val islamicMonthsCache = HashMap<String, List<IslamicMonth>>()

    private val todayKey: String get() = LocalDate.now().toString()

    fun getPrayerTimes(city: String): TodayPrayerTimes? = prayerTimesCache["$city-$todayKey"]
    fun savePrayerTimes(city: String, data: TodayPrayerTimes) {
        prayerTimesCache["$city-$todayKey"] = data
    }

    fun getIslamicMonths(): List<IslamicMonth>? = islamicMonthsCache["months"]
    fun saveIslamicMonths(data: List<IslamicMonth>) {
        islamicMonthsCache["months"] = data
    }
}
