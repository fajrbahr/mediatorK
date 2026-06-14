package com.fajrbahr.mediatork.sample.spring.before.controller

import com.fajrbahr.mediatork.sample.spring.before.domain.GetIslamicMonthsUseCase
import com.fajrbahr.mediatork.sample.spring.before.domain.GetPrayerTimesUseCase
import com.fajrbahr.mediatork.sample.spring.before.model.IslamicMonth
import com.fajrbahr.mediatork.sample.spring.before.model.TodayPrayerTimes
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/before")
class BeforePrayerTimesController(
    private val getPrayerTimes: GetPrayerTimesUseCase,
    private val getIslamicMonths: GetIslamicMonthsUseCase,
) {
    @GetMapping("/prayer-times/{city}")
    suspend fun getPrayerTimes(@PathVariable city: String): TodayPrayerTimes =
        getPrayerTimes(city)

    @GetMapping("/islamic-months")
    suspend fun getIslamicMonths(): List<IslamicMonth> =
        getIslamicMonths()
}
