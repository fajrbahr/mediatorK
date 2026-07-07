package com.fajrbahr.mediatork.sample.spring.islamicmonths

import com.fajrbahr.mediatork.api.Mediator
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class IslamicMonthsController(private val mediator: Mediator) {

    @GetMapping("/islamic-months")
    suspend fun getIslamicMonths(): List<IslamicMonth> =
        mediator.send(GetIslamicMonthsQuery())
}
