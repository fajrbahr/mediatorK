package com.fajrbahr.mediatork.sample.spring.before.data.repository

import com.fajrbahr.mediatork.sample.spring.before.data.cache.AladhanCacheDataSource
import com.fajrbahr.mediatork.sample.spring.before.data.remote.AladhanRemoteDataSource
import com.fajrbahr.mediatork.sample.spring.before.model.IslamicMonth
import com.fajrbahr.mediatork.sample.spring.before.model.TodayPrayerTimes
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Repository

@Repository
class AladhanRepository(
    @Qualifier("beforeRemoteDataSource") private val remote: AladhanRemoteDataSource,
    @Qualifier("beforeCacheDataSource") private val cache: AladhanCacheDataSource,
) {
    suspend fun getPrayerTimesByCity(city: String): TodayPrayerTimes =
        cache.getPrayerTimes(city) ?: remote.getPrayerTimesByCity(city).also { cache.savePrayerTimes(city, it) }

    suspend fun getIslamicMonths(): List<IslamicMonth> =
        cache.getIslamicMonths() ?: remote.getIslamicMonths().also { cache.saveIslamicMonths(it) }
}
