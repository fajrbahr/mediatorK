package com.fajrbahr.mediatork.sample.ktor.prayertimes

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.MediatorRegistrar
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.sample.ktor.AladhanCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class GetPrayerTimesQuery(val city: String, val method: Int = 3) : Request<TodayPrayerTimes>

class GetPrayerTimesHandler(
    private val cache: AladhanCache,
) : RequestHandler<GetPrayerTimesQuery, TodayPrayerTimes> {

    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: GetPrayerTimesQuery,
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

    private fun fetch(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "GET"
            conn.connect()
            conn.inputStream.bufferedReader().readText()
        } finally {
            conn.disconnect()
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

class PrayerTimesRegistrar(private val cache: AladhanCache) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry register GetPrayerTimesHandler(cache)
    }
}
