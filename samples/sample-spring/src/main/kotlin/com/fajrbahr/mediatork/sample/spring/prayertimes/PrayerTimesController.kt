package com.fajrbahr.mediatork.sample.spring.prayertimes

import com.fajrbahr.mediatork.api.Mediator
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class PrayerTimesController(private val mediator: Mediator) {

    @GetMapping("/prayer-times/{city}")
    suspend fun getPrayerTimes(@PathVariable city: String): TodayPrayerTimes =
        mediator.send(GetPrayerTimesQuery(city = city))
}
