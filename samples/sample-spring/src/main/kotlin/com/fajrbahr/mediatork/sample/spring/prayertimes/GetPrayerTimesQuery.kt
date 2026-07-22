package com.fajrbahr.mediatork.sample.spring.prayertimes

import com.fajrbahr.mediatork.Handler
import com.fajrbahr.mediatork.MediatorBuilder
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.sample.spring.AladhanCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class GetPrayerTimesQuery(val city: String, val method: Int = 3) : Request<TodayPrayerTimes>

fun getPrayerTimesHandler(cache: AladhanCache): Handler<GetPrayerTimesQuery, TodayPrayerTimes> = { request ->
    cache.getPrayerTimes(request.city) ?: withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis() / 1000
        parsePrayerTimes(
            fetchPrayerTimes(
                "https://api.aladhan.com/v1/timingsByCity/$timestamp" +
                    "?city=${request.city}&country=&method=${request.method}"
            )
        ).also { cache.savePrayerTimes(request.city, it) }
    }
}

fun MediatorBuilder.prayerTimesModule(cache: AladhanCache) {
    handle(getPrayerTimesHandler(cache))
}

private fun fetchPrayerTimes(url: String): String {
    val conn = URL(url).openConnection() as HttpURLConnection
    return try {
        conn.requestMethod = "GET"
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
