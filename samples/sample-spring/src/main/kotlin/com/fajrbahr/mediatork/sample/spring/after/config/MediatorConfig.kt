package com.fajrbahr.mediatork.sample.spring.after.config

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.MediatorFactory
import com.fajrbahr.mediatork.notification.SilentMissingNotificationHandler
import com.fajrbahr.mediatork.pipeline.buildin.CachingPipelineBehavior
import com.fajrbahr.mediatork.pipeline.buildin.ErrorTrackingPipelineBehavior
import com.fajrbahr.mediatork.pipeline.buildin.LoggingPipelineBehavior
import com.fajrbahr.mediatork.pipeline.buildin.RequestCounterPipelineBehavior
import com.fajrbahr.mediatork.pipeline.buildin.TimeoutPipelineBehavior
import com.fajrbahr.mediatork.pipeline.buildin.TimingPipelineBehavior
import com.fajrbahr.mediatork.sample.spring.after.domain.AppRegistrar
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
            LoggingPipelineBehavior(logger = { msg -> println("[MediatorK] $msg") }, order = -100),
            CachingPipelineBehavior(ttlMs = 30_000, order = -2),
            TimingPipelineBehavior(order = 0) { name, ms -> println("[MediatorK] ⏱ $name took ${ms}ms") },
            TimeoutPipelineBehavior(timeoutMillis = 10_000, order = 10),
            counter,
            ErrorTrackingPipelineBehavior(order = Int.MAX_VALUE) { req, err ->
                println("[MediatorK] ❌ ${req::class.simpleName}: ${err.message}")
            },
        ),
        missingNotificationHandler = SilentMissingNotificationHandler(),
    )
}
