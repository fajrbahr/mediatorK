package com.fajrbahr.mediatork.sample.android.after.domain

import com.fajrbahr.mediatork.Mediator
import com.fajrbahr.mediatork.Request
import com.fajrbahr.mediatork.RequestContext
import com.fajrbahr.mediatork.handler.RequestHandler
import com.fajrbahr.mediatork.sample.android.after.data.cache.AladhanCacheDataSource
import com.fajrbahr.mediatork.sample.android.after.model.PrayerTime
import com.fajrbahr.mediatork.sample.android.after.model.TodayPrayerTimes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class GetPrayerTimesRequest(
    val city: String,
    val method: Int = 3,
) : Request<TodayPrayerTimes>

class GetPrayerTimesHandler(
    private val cache: AladhanCacheDataSource,
) : RequestHandler<GetPrayerTimesRequest, TodayPrayerTimes> {

    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: GetPrayerTimesRequest,
    ): TodayPrayerTimes {
        cache.getPrayerTimes(request.city)?.let { return it }
        return withContext(Dispatchers.IO) {
            val timestamp = System.currentTimeMillis() / 1000
            parse(
                fetch(
                    "https://api.aladhan.com/v1/timingsByCity/$timestamp" +
                            "?city=${request.city}&country=&method=${request.method}"
                )
            ).also { cache.savePrayerTimes(request.city, it) }
        }
    }

    private fun fetch(urlString: String): String {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.connect()
            connection.inputStream.bufferedReader().readText()
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(json: String): TodayPrayerTimes {
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
}
