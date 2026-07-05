package com.fajrbahr.mediatork.sample.android

import com.fajrbahr.mediatork.MediatorFactory
import com.fajrbahr.mediatork.sample.android.after.AladhanCacheDataSource
import com.fajrbahr.mediatork.sample.android.after.times.GetPrayerTimesRequest
import com.fajrbahr.mediatork.sample.android.after.times.prayerTimesRegistrar
import com.fajrbahr.mediatork.sample.android.after.times.PrayerTime
import com.fajrbahr.mediatork.sample.android.after.times.TodayPrayerTimes
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetPrayerTimesHandlerTest {

    private fun makePrayerTimes() = TodayPrayerTimes(
        gregorianDate = "25 June 2026",
        hijriDate = "28 Dhul-Hijjah 1447 AH",
        fajr = PrayerTime("Fajr", "04:28"),
        sunrise = PrayerTime("Sunrise", "05:58"),
        dhuhr = PrayerTime("Dhuhr", "12:29"),
        asr = PrayerTime("Asr", "15:59"),
        maghrib = PrayerTime("Maghrib", "19:00"),
        isha = PrayerTime("Isha", "20:30"),
    )

    @Test
    fun `handler returns cached prayer times without a network call`() = runTest {
        val cache = AladhanCacheDataSource()
        val expected = makePrayerTimes()
        cache.savePrayerTimes("Dubai", expected)

        val mediator = MediatorFactory.create(
            registrars = listOf(prayerTimesRegistrar(cache)),
            verifyHandlers = false,
        )
        val result = mediator.send(GetPrayerTimesRequest(city = "Dubai"))

        assertEquals(expected.gregorianDate, result.gregorianDate)
        assertEquals(expected.fajr, result.fajr)
        assertEquals(6, result.prayers.size)
    }

    @Test
    fun `handler returns correct cached data for the requested city`() = runTest {
        val cache = AladhanCacheDataSource()
        val dubaiTimes = makePrayerTimes()
        cache.savePrayerTimes("Dubai", dubaiTimes)

        val mediator = MediatorFactory.create(
            registrars = listOf(prayerTimesRegistrar(cache)),
            verifyHandlers = false,
        )
        val result = mediator.send(GetPrayerTimesRequest(city = "Dubai"))

        assertEquals("04:28", result.fajr.time)
        assertEquals("Fajr", result.fajr.name)
    }
}
