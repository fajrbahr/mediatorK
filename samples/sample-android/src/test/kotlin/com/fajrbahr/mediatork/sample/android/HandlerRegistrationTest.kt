package com.fajrbahr.mediatork.sample.android

import com.fajrbahr.mediatork.sample.android.after.AladhanCacheDataSource
import com.fajrbahr.mediatork.sample.android.after.islamicMonths.islamicMonthsRegistrar
import com.fajrbahr.mediatork.sample.android.after.times.prayerTimesRegistrar
import com.fajrbahr.mediatork.test.MediatorTestUtils
import kotlin.test.Test

class HandlerRegistrationTest {

    @Test
    fun `all handlers are registered`() {
        val cache = AladhanCacheDataSource()
        MediatorTestUtils.assertAllHandlersRegistered(
            registrars = listOf(
                prayerTimesRegistrar(cache),
                islamicMonthsRegistrar(cache),
            ),
            packages = listOf("com.fajrbahr.mediatork.sample.android"),
        )
    }
}
