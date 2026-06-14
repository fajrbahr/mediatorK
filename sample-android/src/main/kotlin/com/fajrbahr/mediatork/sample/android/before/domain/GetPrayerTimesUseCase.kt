package com.fajrbahr.mediatork.sample.android.before.domain

import com.fajrbahr.mediatork.sample.android.before.data.repository.AladhanRepository
import com.fajrbahr.mediatork.sample.android.before.model.TodayPrayerTimes

class GetPrayerTimesUseCase(private val repository: AladhanRepository) {
    suspend operator fun invoke(
        latitude: Double = 51.5194682,
        longitude: Double = -0.1360365,
        method: Int = 3,
    ): TodayPrayerTimes = repository.getPrayerTimes(latitude, longitude, method)
}
