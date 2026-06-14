package com.fajrbahr.mediatork.sample.spring.before.domain

import com.fajrbahr.mediatork.sample.spring.before.data.repository.AladhanRepository
import com.fajrbahr.mediatork.sample.spring.before.model.TodayPrayerTimes
import org.springframework.stereotype.Service

@Service
class GetPrayerTimesUseCase(private val repository: AladhanRepository) {
    suspend operator fun invoke(city: String): TodayPrayerTimes =
        repository.getPrayerTimesByCity(city)
}
