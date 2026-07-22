package com.fajrbahr.mediatork.sample.android.after.times

import com.fajrbahr.mediatork.Handler
import com.fajrbahr.mediatork.MediatorBuilder
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.sample.android.after.AladhanCacheDataSource
import com.fajrbahr.mediatork.validator.rules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class GetPrayerTimesRequest(
    val city: String,
    val country: String = "",
    val method: Int = 3,
) : Request<TodayPrayerTimes> {
    override fun validate() = rules<String> {
        check(city.isNotBlank()) { "City must not be blank" }
        check(city.length >= 2) { "City must be at least 2 characters" }
        check(city.all { it.isLetter() || it.isWhitespace() || it == '-' }) {
            "City must contain only letters, spaces, or hyphens"
        }
        if (country.isNotEmpty()) {
            check(country.length >= 2) { "Country must be at least 2 characters" }
            check(country.all { it.isLetter() || it.isWhitespace() || it == '-' }) {
                "Country must contain only letters, spaces, or hyphens"
            }
        }
    }
}

fun getPrayerTimesHandler(cache: AladhanCacheDataSource): Handler<GetPrayerTimesRequest, TodayPrayerTimes> = { request ->
    cache.getPrayerTimes(request.city) ?: withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis() / 1000
        parsePrayerTimes(
            fetchPrayerTimes(
                "https://api.aladhan.com/v1/timingsByCity/$timestamp" +
                    "?city=${request.city}&country=${request.country}&method=${request.method}"
            )
        ).also { cache.savePrayerTimes(request.city, it) }
    }
}

fun MediatorBuilder.prayerTimesModule(cache: AladhanCacheDataSource) {
    handle(getPrayerTimesHandler(cache))
}

private fun fetchPrayerTimes(urlString: String): String {
    val connection = URL(urlString).openConnection() as HttpURLConnection
    return try {
        connection.requestMethod = "GET"
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
