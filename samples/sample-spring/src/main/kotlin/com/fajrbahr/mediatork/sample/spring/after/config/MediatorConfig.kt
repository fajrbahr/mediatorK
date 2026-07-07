package com.fajrbahr.mediatork.sample.spring.after.config

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.MediatorFactory
import com.fajrbahr.mediatork.notification.SilentMissingNotificationHandler
import com.fajrbahr.mediatork.pipeline.buildin.AuthorizationPipelineBehavior
import com.fajrbahr.mediatork.pipeline.buildin.CachingPipelineBehavior
import com.fajrbahr.mediatork.pipeline.buildin.CircuitBreakerPipelineBehavior
import com.fajrbahr.mediatork.pipeline.buildin.DeduplicationPipelineBehavior
import com.fajrbahr.mediatork.pipeline.buildin.ErrorTrackingPipelineBehavior
import com.fajrbahr.mediatork.pipeline.buildin.LoggingPipelineBehavior
import com.fajrbahr.mediatork.pipeline.buildin.RateLimitPipelineBehavior
import com.fajrbahr.mediatork.pipeline.buildin.RequestCounterPipelineBehavior
import com.fajrbahr.mediatork.pipeline.buildin.RetryPipelineBehavior
import com.fajrbahr.mediatork.pipeline.buildin.TimeoutPipelineBehavior
import com.fajrbahr.mediatork.pipeline.buildin.TimingPipelineBehavior
import com.fajrbahr.mediatork.sample.spring.after.domain.AppRegistrar
import com.fajrbahr.mediatork.sample.spring.after.domain.GetPrayerTimesRequest
import com.fajrbahr.mediatork.sample.spring.aftersuper.domain.AfterSuperRegistrar
import com.fajrbahr.mediatork.sample.spring.aftersuper.domain.GetPrayerTimesValidator
import com.fajrbahr.mediatork.sample.spring.aftersuper.domain.RequestAuditBehavior
import com.fajrbahr.mediatork.sample.spring.aftersuper.domain.TraceIdBehavior
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
        afterSuperRegistrar: AfterSuperRegistrar,
        counter: RequestCounterPipelineBehavior,
    ): Mediator = MediatorFactory.create(
        registrars = listOf(afterSuperRegistrar),
        pipelineBehaviors = listOf(
            // -200: outermost — wraps each retry attempt
            RetryPipelineBehavior(maxRetries = 2, delayMillis = 200, order = -200),
            // -100: log entry/exit per attempt
            LoggingPipelineBehavior(logger = { msg -> println("[MediatorK] $msg") }, order = -100),
            // -50: fail fast before any expensive processing
            ValidationBehavior(listOf(GetPrayerTimesValidator()), order = -50),
            // -10: authorization — only fires for AuthenticatedRequest types
            AuthorizationPipelineBehavior(order = -10) { context, request ->
                val traceId = context.getMetaData<String>("traceId") ?: "no-trace"
                println("[Auth] Authorized ${request::class.simpleName} — traceId=$traceId")
            },
            // -5: rate limit after auth
            RateLimitPipelineBehavior(maxRequests = 10, windowMs = 10_000, order = -5),
            // -3: deduplicate concurrent identical requests
            DeduplicationPipelineBehavior(
                keyFor = { req -> "${req::class.simpleName}:${(req as? GetPrayerTimesRequest)?.city ?: req.toString()}" },
                order = -3,
            ),
            // -2: cache results — hits skip the handler entirely
            CachingPipelineBehavior(ttlMs = 30_000, order = -2),
            // 0: time actual handler execution
            TimingPipelineBehavior(order = 0) { name, ms -> println("[MediatorK] ⏱ $name took ${ms}ms") },
            // 10: cancel if handler exceeds deadline
            TimeoutPipelineBehavior(timeoutMillis = 10_000, order = 10),
            // 15: trip circuit after 5 consecutive failures
            CircuitBreakerPipelineBehavior(
                failureThreshold = 5,
                resetTimeoutMs = 15_000,
                onStateChange = { state -> println("[MediatorK] Circuit: $state") },
                order = 15,
            ),
            // 20: count actual dispatches per request type
            counter,
            // Int.MAX_VALUE: innermost — tracks errors closest to the handler
            ErrorTrackingPipelineBehavior(order = Int.MAX_VALUE) { req, err ->
                println("[MediatorK] ❌ ${req::class.simpleName}: ${err.message}")
            },
            TraceIdBehavior(),
            RequestAuditBehavior(),
        ),
        missingNotificationHandler = SilentMissingNotificationHandler(),
    )
}
