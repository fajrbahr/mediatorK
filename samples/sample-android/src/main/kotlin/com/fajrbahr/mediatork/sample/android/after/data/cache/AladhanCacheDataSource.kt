package com.fajrbahr.mediatork.sample.android.after.data.cache

import com.fajrbahr.mediatork.sample.android.after.model.IslamicMonth
import com.fajrbahr.mediatork.sample.android.after.model.TodayPrayerTimes
import java.util.Calendar

class AladhanCacheDataSource {
    private val prayerTimesCache = HashMap<String, TodayPrayerTimes>()
    private val islamicMonthsCache = HashMap<String, List<IslamicMonth>>()

    private val todayKey: String
        get() {
            val cal = Calendar.getInstance()
            return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
        }

    fun getPrayerTimes(city: String): TodayPrayerTimes? = prayerTimesCache["$city-$todayKey"]
    fun savePrayerTimes(city: String, data: TodayPrayerTimes) {
        prayerTimesCache["$city-$todayKey"] = data
    }

    fun getIslamicMonths(): List<IslamicMonth>? = islamicMonthsCache["months"]
    fun saveIslamicMonths(data: List<IslamicMonth>) {
        islamicMonthsCache["months"] = data
    }
}
