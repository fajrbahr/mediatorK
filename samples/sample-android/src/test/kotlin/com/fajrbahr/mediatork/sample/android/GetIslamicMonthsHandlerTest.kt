package com.fajrbahr.mediatork.sample.android

import com.fajrbahr.mediatork.mediatorK
import com.fajrbahr.mediatork.sample.android.after.AladhanCacheDataSource
import com.fajrbahr.mediatork.sample.android.after.islamicMonths.GetIslamicMonthsRequest
import com.fajrbahr.mediatork.sample.android.after.islamicMonths.IslamicMonth
import com.fajrbahr.mediatork.sample.android.after.islamicMonths.islamicMonthsModule
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetIslamicMonthsHandlerTest {

    private val twelveMonths = (1..12).map { i ->
        IslamicMonth(number = i, nameEn = "Month$i", nameAr = "شهر$i")
    }

    @Test
    fun `handler returns cached months without a network call`() = runTest {
        val cache = AladhanCacheDataSource()
        cache.saveIslamicMonths(twelveMonths)

        val mediator = mediatorK { islamicMonthsModule(cache) }
        val result = mediator.send(GetIslamicMonthsRequest())

        assertEquals(12, result.size)
        assertEquals(1, result.first().number)
        assertEquals(12, result.last().number)
    }

    @Test
    fun `handler returns exact cached data in order`() = runTest {
        val cache = AladhanCacheDataSource()
        val months = listOf(
            IslamicMonth(1, "Muharram", "محرم"),
            IslamicMonth(2, "Safar", "صفر"),
        )
        cache.saveIslamicMonths(months)

        val mediator = mediatorK { islamicMonthsModule(cache) }
        val result = mediator.send(GetIslamicMonthsRequest())

        assertEquals("Muharram", result[0].nameEn)
        assertEquals("Safar", result[1].nameEn)
    }
}
