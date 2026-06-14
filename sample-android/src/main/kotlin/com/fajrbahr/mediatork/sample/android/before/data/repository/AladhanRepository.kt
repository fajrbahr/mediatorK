package com.fajrbahr.mediatork.sample.android.before.data.repository

import com.fajrbahr.mediatork.sample.android.before.data.cache.AladhanCacheDataSource
import com.fajrbahr.mediatork.sample.android.before.data.remote.AladhanRemoteDataSource
import com.fajrbahr.mediatork.sample.android.before.model.IslamicMonth
import com.fajrbahr.mediatork.sample.android.before.model.TodayPrayerTimes

class AladhanRepository(
    private val dataSource: AladhanRemoteDataSource,
    private val cache: AladhanCacheDataSource,
) {
    suspend fun getPrayerTimes(
        latitude: Double = 51.5194682,
        longitude: Double = -0.1360365,
        method: Int = 3,
    ): TodayPrayerTimes = cache.getPrayerTimes() ?: run {
        dataSource.getPrayerTimes(latitude, longitude, method).also { cache.savePrayerTimes(it) }
    }

    suspend fun getIslamicMonths(): List<IslamicMonth> = cache.getIslamicMonths() ?: run {
        dataSource.getIslamicMonths().also { cache.saveIslamicMonths(it) }
    }
}
