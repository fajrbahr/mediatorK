package com.fajrbahr.mediatork.sample.ktor.before.data.repository

import com.fajrbahr.mediatork.sample.ktor.before.data.cache.AladhanCacheDataSource
import com.fajrbahr.mediatork.sample.ktor.before.data.remote.AladhanRemoteDataSource
import com.fajrbahr.mediatork.sample.ktor.before.model.IslamicMonth
import com.fajrbahr.mediatork.sample.ktor.before.model.TodayPrayerTimes

class AladhanRepository(
    private val remote: AladhanRemoteDataSource,
    private val cache: AladhanCacheDataSource,
) {
    suspend fun getPrayerTimesByCity(city: String): TodayPrayerTimes =
        cache.getPrayerTimes(city) ?: remote.getPrayerTimesByCity(city).also { cache.savePrayerTimes(city, it) }

    suspend fun getIslamicMonths(): List<IslamicMonth> =
        cache.getIslamicMonths() ?: remote.getIslamicMonths().also { cache.saveIslamicMonths(it) }
}
