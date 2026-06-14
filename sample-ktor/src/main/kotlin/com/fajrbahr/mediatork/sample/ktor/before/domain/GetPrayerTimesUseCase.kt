package com.fajrbahr.mediatork.sample.ktor.before.domain

import com.fajrbahr.mediatork.sample.ktor.before.data.repository.AladhanRepository
import com.fajrbahr.mediatork.sample.ktor.before.model.TodayPrayerTimes

class GetPrayerTimesUseCase(private val repository: AladhanRepository) {
    suspend operator fun invoke(city: String): TodayPrayerTimes =
        repository.getPrayerTimesByCity(city)
}
