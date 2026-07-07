package com.fajrbahr.mediatork.sample.spring

import com.fajrbahr.mediatork.sample.spring.islamicmonths.IslamicMonth
import com.fajrbahr.mediatork.sample.spring.prayertimes.TodayPrayerTimes
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class AladhanCache {
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
