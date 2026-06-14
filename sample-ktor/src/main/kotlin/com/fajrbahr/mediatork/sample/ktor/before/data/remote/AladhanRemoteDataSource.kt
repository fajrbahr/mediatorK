package com.fajrbahr.mediatork.sample.ktor.before.data.remote

import com.fajrbahr.mediatork.sample.ktor.before.model.IslamicMonth
import com.fajrbahr.mediatork.sample.ktor.before.model.PrayerTime
import com.fajrbahr.mediatork.sample.ktor.before.model.TodayPrayerTimes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AladhanRemoteDataSource {

    suspend fun getPrayerTimesByCity(city: String, method: Int = 3): TodayPrayerTimes =
        withContext(Dispatchers.IO) {
            val timestamp = System.currentTimeMillis() / 1000
            parsePrayerTimes(
                fetch("https://api.aladhan.com/v1/timingsByCity/$timestamp?city=$city&country=&method=$method")
            )
        }

    suspend fun getIslamicMonths(): List<IslamicMonth> =
        withContext(Dispatchers.IO) {
            parseIslamicMonths(
                fetch("https://api.aladhan.com/v1/islamicMonths", headers = mapOf("Accept-Encoding" to ""))
            )
        }

    private fun fetch(url: String, headers: Map<String, String> = emptyMap()): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "GET"
            headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            conn.connect()
            conn.inputStream.bufferedReader().readText()
        } finally {
            conn.disconnect()
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
                nameAr = month.getString("ar"),
            )
        }
    }
}
