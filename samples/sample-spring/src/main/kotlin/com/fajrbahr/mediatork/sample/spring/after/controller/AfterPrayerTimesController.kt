package com.fajrbahr.mediatork.sample.spring.after.controller

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.sample.spring.after.domain.GetIslamicMonthsRequest
import com.fajrbahr.mediatork.sample.spring.after.domain.GetPrayerTimesRequest
import com.fajrbahr.mediatork.sample.spring.after.model.IslamicMonth
import com.fajrbahr.mediatork.sample.spring.after.model.TodayPrayerTimes
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/after")
class AfterPrayerTimesController(
    @Qualifier("mediator") private val mediator: Mediator,
) {
    @GetMapping("/prayer-times/{city}")
    suspend fun getPrayerTimes(@PathVariable city: String): TodayPrayerTimes =
        mediator.send(GetPrayerTimesRequest(city = city))

    @GetMapping("/islamic-months")
    suspend fun getIslamicMonths(): List<IslamicMonth> =
        mediator.send(GetIslamicMonthsRequest())
}
