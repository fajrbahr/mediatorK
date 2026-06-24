package com.fajrbahr.mediatork.sample.android

import com.fajrbahr.mediatork.MediatorFactory
import com.fajrbahr.mediatork.sample.android.after.data.cache.AladhanCacheDataSource
import com.fajrbahr.mediatork.sample.android.after.domain.GetIslamicMonthsRequest
import com.fajrbahr.mediatork.sample.android.after.domain.IslamicMonthsRegistrar
import com.fajrbahr.mediatork.sample.android.after.model.IslamicMonth
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [GetIslamicMonthsHandler] exercising the cache-hit path.
 * No network call is made — the cache is pre-populated in each test.
 */
class GetIslamicMonthsHandlerTest {

    private val twelveMonths = (1..12).map { i ->
        IslamicMonth(number = i, nameEn = "Month$i", nameAr = "شهر$i")
    }

    @Test
    fun `handler returns cached months without a network call`() = runTest {
        val cache = AladhanCacheDataSource()
        cache.saveIslamicMonths(twelveMonths)

        val mediator = MediatorFactory.create(
            registrars = listOf(IslamicMonthsRegistrar(cache)),
            verifyHandlers = false,
        )
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

        val mediator = MediatorFactory.create(
            registrars = listOf(IslamicMonthsRegistrar(cache)),
            verifyHandlers = false,
        )
        val result = mediator.send(GetIslamicMonthsRequest())

        assertEquals("Muharram", result[0].nameEn)
        assertEquals("Safar", result[1].nameEn)
    }
}
