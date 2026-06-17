package com.fajrbahr.mediatork.sample.android.before.data.remote

import com.fajrbahr.mediatork.sample.android.before.model.IslamicMonth
import com.fajrbahr.mediatork.sample.android.before.model.PrayerTime
import com.fajrbahr.mediatork.sample.android.before.model.TodayPrayerTimes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AladhanRemoteDataSource(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun getPrayerTimes(
        latitude: Double,
        longitude: Double,
        method: Int,
    ): TodayPrayerTimes = withContext(ioDispatcher) {
        val timestamp = System.currentTimeMillis() / 1000
        parsePrayerTimes(
            fetch(
                "https://api.aladhan.com/v1/timings/$timestamp" +
                        "?latitude=$latitude&longitude=$longitude&method=$method" +
                        "&shafaq=general&tune=5%2C3%2C5%2C7%2C9%2C-1%2C0%2C8%2C-6" +
                        "&school=0&midnightMode=0&timezonestring=UTC" +
                        "&latitudeAdjustmentMethod=1&calendarMethod=UAQ&iso8601=false"
            )
        )
    }

    suspend fun getPrayerTimesByCity(
        city: String,
        method: Int = 3,
    ): TodayPrayerTimes = withContext(ioDispatcher) {
        val timestamp = System.currentTimeMillis() / 1000
        parsePrayerTimes(
            fetch("https://api.aladhan.com/v1/timingsByCity/$timestamp?city=$city&country=&method=$method")
        )
    }

    suspend fun getIslamicMonths(): List<IslamicMonth> = withContext(ioDispatcher) {
        parseIslamicMonths(
            fetch("https://api.aladhan.com/v1/islamicMonths", headers = mapOf("Accept-Encoding" to ""))
        )
    }

    private fun fetch(urlString: String, headers: Map<String, String> = emptyMap()): String {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            headers.forEach { (k, v) -> connection.setRequestProperty(k, v) }
            connection.connect()
            connection.inputStream.bufferedReader().readText()
        } finally {
            connection.disconnect()
        }
    }

    private fun parsePrayerTimes(json: String): TodayPrayerTimes {
        val data = JSONObject(json).getJSONObject("data")
        val timings = data.getJSONObject("timings")
        val gregorian = data.getJSONObject("date").getJSONObject("gregorian")
        val hijri = data.getJSONObject("date").getJSONObject("hijri")
        fun prayer(key: String) = PrayerTime(key, timings.getString(key))
        return TodayPrayerTimes(
            gregorianDate = "${gregorian.getString("day")} " +
                    "${gregorian.getJSONObject("month").getString("en")} " +
                    gregorian.getString("year"),
            hijriDate = "${hijri.getString("day")} " +
                    "${hijri.getJSONObject("month").getString("en")} " +
                    "${hijri.getString("year")} AH",
            fajr = prayer("Fajr"), sunrise = prayer("Sunrise"),
            dhuhr = prayer("Dhuhr"), asr = prayer("Asr"),
            maghrib = prayer("Maghrib"), isha = prayer("Isha"),
        )
    }

    private fun parseIslamicMonths(json: String): List<IslamicMonth> {
        val data = JSONObject(json).getJSONObject("data")
        return (1..12).map { i ->
            val month = data.getJSONObject(i.toString())
            IslamicMonth(
                number = month.getInt("number"),
                nameEn = month.getString("en"),
                nameAr = month.getString("ar")
            )
        }
    }
}
