package com.fajrbahr.mediatork.sample.spring.aftersuper.controller

import com.fajrbahr.mediatork.Mediator
import com.fajrbahr.mediatork.pipeline.RequestCounterPipelineBehavior
import com.fajrbahr.mediatork.sample.spring.after.domain.GetIslamicMonthsRequest
import com.fajrbahr.mediatork.sample.spring.after.domain.GetPrayerTimesRequest
import com.fajrbahr.mediatork.sample.spring.aftersuper.model.AfterSuperIslamicMonthsResponse
import com.fajrbahr.mediatork.sample.spring.aftersuper.model.AfterSuperPrayerTimesResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/aftersuper")
class AfterSuperPrayerTimesController(
    @Qualifier("mediatorWithBehaviors") private val mediator: Mediator,
    private val counter: RequestCounterPipelineBehavior,
) {
    @GetMapping("/prayer-times/{city}")
    suspend fun getPrayerTimes(@PathVariable city: String): AfterSuperPrayerTimesResponse {
        val prayerTimes = mediator.send(GetPrayerTimesRequest(city = city))
        return AfterSuperPrayerTimesResponse(
            prayerTimes = prayerTimes,
            requestCount = counter.countFor(GetPrayerTimesRequest::class),
        )
    }

    @GetMapping("/islamic-months")
    suspend fun getIslamicMonths(): AfterSuperIslamicMonthsResponse {
        val months = mediator.send(GetIslamicMonthsRequest())
        return AfterSuperIslamicMonthsResponse(
            months = months,
            requestCount = counter.countFor(GetIslamicMonthsRequest::class),
        )
    }
}
