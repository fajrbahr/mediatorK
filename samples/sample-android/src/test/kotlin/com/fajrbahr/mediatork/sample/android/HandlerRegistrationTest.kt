package com.fajrbahr.mediatork.sample.android

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.sample.android.after.data.cache.AladhanCacheDataSource
import com.fajrbahr.mediatork.sample.android.after.domain.IslamicMonthsRegistrar
import com.fajrbahr.mediatork.sample.android.after.domain.PrayerTimesRegistrar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifies that each registrar correctly registers its handler into the registry.
 * Mirrors the HandlerCoverageTest pattern from the pure-JVM sample.
 */
class HandlerRegistrationTest {

    @Test
    fun `PrayerTimesRegistrar registers GetPrayerTimesHandler`() {
        val registry = HandlerRegistry()
        PrayerTimesRegistrar(AladhanCacheDataSource()).register(registry)
        assertTrue(registry.registeredRequestTypes().isNotEmpty())
    }

    @Test
    fun `IslamicMonthsRegistrar registers GetIslamicMonthsHandler`() {
        val registry = HandlerRegistry()
        IslamicMonthsRegistrar(AladhanCacheDataSource()).register(registry)
        assertTrue(registry.registeredRequestTypes().isNotEmpty())
    }

    @Test
    fun `mediator built from both registrars has two request handlers`() {
        val cache = AladhanCacheDataSource()
        val registry = HandlerRegistry()
        PrayerTimesRegistrar(cache).register(registry)
        IslamicMonthsRegistrar(cache).register(registry)
        assertEquals(2, registry.registeredRequestTypes().size)
    }
}
