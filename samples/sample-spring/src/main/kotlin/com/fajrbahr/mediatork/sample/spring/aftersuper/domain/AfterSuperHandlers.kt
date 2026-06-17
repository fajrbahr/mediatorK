package com.fajrbahr.mediatork.sample.spring.aftersuper.domain

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.Mediator
import com.fajrbahr.mediatork.MediatorRegistrar
import com.fajrbahr.mediatork.RequestContext
import com.fajrbahr.mediatork.handler.RequestExceptionHandler
import com.fajrbahr.mediatork.handler.RequestHandler
import com.fajrbahr.mediatork.handler.otherwise
import com.fajrbahr.mediatork.notification.otherwise
import com.fajrbahr.mediatork.sample.spring.after.data.cache.AladhanCacheDataSource
import com.fajrbahr.mediatork.sample.spring.after.domain.GetIslamicMonthsHandler
import com.fajrbahr.mediatork.sample.spring.after.domain.GetPrayerTimesRequest
import com.fajrbahr.mediatork.sample.spring.after.model.PrayerTime
import com.fajrbahr.mediatork.sample.spring.after.model.TodayPrayerTimes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.springframework.stereotype.Component
import java.net.HttpURLConnection
import java.net.URL

/** Reads traceId from [RequestContext] and publishes [PrayerTimesFetchedNotification] after each fetch. */
class AfterSuperPrayerTimesHandler(
    private val cache: AladhanCacheDataSource,
) : RequestHandler<GetPrayerTimesRequest, TodayPrayerTimes> {

    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: GetPrayerTimesRequest,
    ): TodayPrayerTimes {
        val traceId = requestContext.getMetaDate<String>("traceId") ?: "no-trace"
        println("[Handler] [$traceId] Fetching prayer times for '${request.city}'")

        cache.getPrayerTimes(request.city)?.let {
            mediator.publish(PrayerTimesFetchedNotification(city = request.city, source = "cache"))
            return it
        }

        return withContext(Dispatchers.IO) {
            val ts = System.currentTimeMillis() / 1000
            parse(
                fetch(
                    "https://api.aladhan.com/v1/timingsByCity/$ts" +
                            "?city=${request.city}&country=&method=${request.method}"
                )
            ).also {
                cache.savePrayerTimes(request.city, it)
                mediator.publish(PrayerTimesFetchedNotification(city = request.city, source = "network"))
            }
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
        fun p(key: String) = PrayerTime(key, timings.getString(key))
        return TodayPrayerTimes(
            gregorianDate = "${gregorian.getString("day")} " +
                    "${gregorian.getJSONObject("month").getString("en")} " +
                    gregorian.getString("year"),
            hijriDate = "${hijri.getString("day")} " +
                    "${hijri.getJSONObject("month").getString("en")} " +
                    "${hijri.getString("year")} AH",
            fajr = p("Fajr"), sunrise = p("Sunrise"), dhuhr = p("Dhuhr"),
            asr = p("Asr"), maghrib = p("Maghrib"), isha = p("Isha"),
        )
    }
}

/** Returns placeholder prayer times when the primary handler fails — demonstrates FallbackRequestHandler. */
class AfterSuperPrayerTimesFallbackHandler : RequestHandler<GetPrayerTimesRequest, TodayPrayerTimes> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: GetPrayerTimesRequest,
    ): TodayPrayerTimes {
        println("[Fallback] Primary failed — returning stub times for '${request.city}'")
        val na = { name: String -> PrayerTime(name, "—") }
        return TodayPrayerTimes(
            gregorianDate = "—", hijriDate = "—",
            fajr = na("Fajr"), sunrise = na("Sunrise"), dhuhr = na("Dhuhr"),
            asr = na("Asr"), maghrib = na("Maghrib"), isha = na("Isha"),
        )
    }
}

/** Converts unhandled exceptions into a placeholder result — demonstrates RequestExceptionHandler. */
class PrayerTimesExceptionHandler : RequestExceptionHandler<GetPrayerTimesRequest, TodayPrayerTimes, Exception> {
    override suspend fun handle(
        requestContext: RequestContext,
        request: GetPrayerTimesRequest,
        exception: Exception,
    ): TodayPrayerTimes {
        println("[ExceptionHandler] Error for '${request.city}': ${exception.message}")
        val err = { name: String -> PrayerTime(name, "N/A") }
        return TodayPrayerTimes(
            gregorianDate = "Error", hijriDate = "Error",
            fajr = err("Fajr"), sunrise = err("Sunrise"), dhuhr = err("Dhuhr"),
            asr = err("Asr"), maghrib = err("Maghrib"), isha = err("Isha"),
        )
    }
}

/**
 * Registrar for the aftersuper layer. Demonstrates every registration pattern:
 * - [otherwise] for [RequestHandler] → [com.fajrbahr.mediatork.handler.FallbackRequestHandler]
 * - [otherwise] for [com.fajrbahr.mediatork.notification.NotificationHandler] → [com.fajrbahr.mediatork.notification.FallbackNotificationHandler]
 * - [HandlerRegistry.registerExceptionHandler]
 */
@Component
class AfterSuperRegistrar(private val cache: AladhanCacheDataSource) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.scope {
            +(AfterSuperPrayerTimesHandler(cache) otherwise AfterSuperPrayerTimesFallbackHandler())
            +GetIslamicMonthsHandler(cache)
            registerNotification(LogPrayerTimesFetchedHandler() otherwise AnalyticsPrayerTimesFetchedHandler())
            registerExceptionHandler(
                requestClass = GetPrayerTimesRequest::class,
                exceptionClass = Exception::class,
                handler = PrayerTimesExceptionHandler(),
            )
        }
    }
}
