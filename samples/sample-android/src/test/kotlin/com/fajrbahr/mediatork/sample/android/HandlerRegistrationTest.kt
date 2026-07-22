package com.fajrbahr.mediatork.sample.android

import com.fajrbahr.mediatork.mediatorK
import com.fajrbahr.mediatork.sample.android.after.AladhanCacheDataSource
import com.fajrbahr.mediatork.sample.android.after.islamicMonths.GetIslamicMonthsRequest
import com.fajrbahr.mediatork.sample.android.after.islamicMonths.IslamicMonth
import com.fajrbahr.mediatork.sample.android.after.islamicMonths.islamicMonthsModule
import com.fajrbahr.mediatork.sample.android.after.times.PrayerTime
import com.fajrbahr.mediatork.sample.android.after.times.GetPrayerTimesRequest
import com.fajrbahr.mediatork.sample.android.after.times.TodayPrayerTimes
import com.fajrbahr.mediatork.sample.android.after.times.prayerTimesModule
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/**
 * The FP replacement for registry verification: assemble the real modules and confirm every
 * request type resolves to a handler. A missing handler would throw MissingHandlerException.
 * The cache is pre-seeded so no network call is made.
 */
class HandlerRegistrationTest {

    @Test
    fun `all handlers are registered`() = runTest {
        val cache = AladhanCacheDataSource().apply {
            saveIslamicMonths(listOf(IslamicMonth(1, "Muharram", "محرم")))
            savePrayerTimes(
                "Dubai",
                TodayPrayerTimes(
                    gregorianDate = "25 June 2026",
                    hijriDate = "28 Dhul-Hijjah 1447 AH",
                    fajr = PrayerTime("Fajr", "04:28"),
                    sunrise = PrayerTime("Sunrise", "05:58"),
                    dhuhr = PrayerTime("Dhuhr", "12:29"),
                    asr = PrayerTime("Asr", "15:59"),
                    maghrib = PrayerTime("Maghrib", "19:00"),
                    isha = PrayerTime("Isha", "20:30"),
                ),
            )
        }

        val mediator = mediatorK {
            prayerTimesModule(cache)
            islamicMonthsModule(cache)
        }

        // Each send resolving without a MissingHandlerException proves the handler is wired.
        mediator.send(GetIslamicMonthsRequest())
        mediator.send(GetPrayerTimesRequest(city = "Dubai"))
    }
}
