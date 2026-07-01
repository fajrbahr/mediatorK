package com.fajrbahr.mediatork.sample.android

import com.fajrbahr.mediatork.sample.android.after.AladhanCacheDataSource
import com.fajrbahr.mediatork.sample.android.after.islamicMonths.IslamicMonth
import com.fajrbahr.mediatork.sample.android.after.times.PrayerTime
import com.fajrbahr.mediatork.sample.android.after.times.TodayPrayerTimes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for [AladhanCacheDataSource] — verifies in-memory cache operations without any
 * network calls.  Pure JVM, no mocking.
 */
class AladhanCacheDataSourceTest {

    private val cache = AladhanCacheDataSource()

    private fun fakePrayerTimes(city: String) = TodayPrayerTimes(
        gregorianDate = "25 June 2026",
        hijriDate = "28 Dhul-Hijjah 1447 AH",
        fajr = PrayerTime("Fajr", "04:28"),
        sunrise = PrayerTime("Sunrise", "05:58"),
        dhuhr = PrayerTime("Dhuhr", "12:29"),
        asr = PrayerTime("Asr", "15:59"),
        maghrib = PrayerTime("Maghrib", "19:00"),
        isha = PrayerTime("Isha", "20:30"),
    )

    // ── Prayer times ──────────────────────────────────────────────────────────

    @Test
    fun `getPrayerTimes returns null before any save`() {
        assertNull(cache.getPrayerTimes("Dubai"))
    }

    @Test
    fun `saved prayer times are returned for the same city`() {
        val times = fakePrayerTimes("Dubai")
        cache.savePrayerTimes("Dubai", times)
        assertEquals(times, cache.getPrayerTimes("Dubai"))
    }

    @Test
    fun `different cities are cached independently`() {
        val dubai = fakePrayerTimes("Dubai")
        val riyadh = fakePrayerTimes("Riyadh")
        cache.savePrayerTimes("Dubai", dubai)
        cache.savePrayerTimes("Riyadh", riyadh)
        assertEquals(dubai, cache.getPrayerTimes("Dubai"))
        assertEquals(riyadh, cache.getPrayerTimes("Riyadh"))
    }

    @Test
    fun `getPrayerTimes returns null for unknown city even after other cities saved`() {
        cache.savePrayerTimes("Dubai", fakePrayerTimes("Dubai"))
        assertNull(cache.getPrayerTimes("Makkah"))
    }

    // ── Islamic months ────────────────────────────────────────────────────────

    @Test
    fun `getIslamicMonths returns null before any save`() {
        assertNull(cache.getIslamicMonths())
    }

    @Test
    fun `saved islamic months are returned`() {
        val months = listOf(
            IslamicMonth(1, "Muharram", "محرم"),
            IslamicMonth(2, "Safar", "صفر"),
        )
        cache.saveIslamicMonths(months)
        assertEquals(months, cache.getIslamicMonths())
    }
}
