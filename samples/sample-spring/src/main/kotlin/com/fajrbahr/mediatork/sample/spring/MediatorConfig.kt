package com.fajrbahr.mediatork.sample.spring

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.mediatorK
import com.fajrbahr.mediatork.sample.spring.islamicmonths.islamicMonthsModule
import com.fajrbahr.mediatork.sample.spring.prayertimes.prayerTimesModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MediatorConfig {

    @Bean
    fun mediator(cache: AladhanCache): Mediator = mediatorK {
        prayerTimesModule(cache)
        islamicMonthsModule(cache)
    }
}
