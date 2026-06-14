package com.fajrbahr.mediatork.sample.spring.after.config

import com.fajrbahr.mediatork.Mediator
import com.fajrbahr.mediatork.MediatorFactory
import com.fajrbahr.mediatork.pipeline.ErrorTrackingPipelineBehavior
import com.fajrbahr.mediatork.pipeline.LoggingPipelineBehavior
import com.fajrbahr.mediatork.pipeline.RequestCounterPipelineBehavior
import com.fajrbahr.mediatork.pipeline.RetryPipelineBehavior
import com.fajrbahr.mediatork.pipeline.TimeoutPipelineBehavior
import com.fajrbahr.mediatork.pipeline.TimingPipelineBehavior
import com.fajrbahr.mediatork.sample.spring.after.domain.AppRegistrar
import com.fajrbahr.mediatork.sample.spring.aftersuper.domain.GetPrayerTimesValidator
import com.fajrbahr.mediatork.validator.ValidationBehavior
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MediatorConfig {

    @Bean("mediator")
    fun mediator(registrar: AppRegistrar): Mediator =
        MediatorFactory.create(registrars = listOf(registrar))

    @Bean
    fun requestCounter(): RequestCounterPipelineBehavior =
        RequestCounterPipelineBehavior(order = 20)

    @Bean("mediatorWithBehaviors")
    fun mediatorWithBehaviors(
        registrar: AppRegistrar,
        counter: RequestCounterPipelineBehavior,
    ): Mediator = MediatorFactory.create(
        registrars = listOf(registrar),
        pipelineBehaviors = listOf(
            ValidationBehavior(listOf(GetPrayerTimesValidator())),
            RetryPipelineBehavior(maxRetries = 2, delayMillis = 200, order = -200),
            LoggingPipelineBehavior(logger = { msg -> println("[MediatorK] $msg") }, order = -100),
            TimingPipelineBehavior(order = 0) { name, ms -> println("[MediatorK] ⏱ $name took ${ms}ms") },
            TimeoutPipelineBehavior(timeoutMillis = 10_000, order = 10),
            counter,
            ErrorTrackingPipelineBehavior(order = Int.MAX_VALUE) { req, err ->
                println("[MediatorK] ❌ ${req::class.simpleName}: ${err.message}")
            },
        ),
    )
}
