package com.fajrbahr.mediatork.sample.android

import com.fajrbahr.mediatork.sample.android.after.AladhanCacheDataSource
import com.fajrbahr.mediatork.sample.android.after.islamicMonths.IslamicMonthsRegistrar
import com.fajrbahr.mediatork.sample.android.after.times.PrayerTimesRegistrar
import com.fajrbahr.mediatork.test.MediatorTestUtils
import kotlin.test.Test

class HandlerRegistrationTest {

    @Test
    fun `all handlers are registered`() {
        val cache = AladhanCacheDataSource()
        MediatorTestUtils.assertAllHandlersRegistered(
            registrars = listOf(
                PrayerTimesRegistrar(cache),
                IslamicMonthsRegistrar(cache),
            ),
            packages = listOf("com.fajrbahr.mediatork.sample.android"),
        )
    }
}
