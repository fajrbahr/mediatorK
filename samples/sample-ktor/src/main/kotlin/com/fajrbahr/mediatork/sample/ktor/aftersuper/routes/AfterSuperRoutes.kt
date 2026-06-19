package com.fajrbahr.mediatork.sample.ktor.aftersuper.routes

import com.fajrbahr.mediatork.MediatorFactory
import com.fajrbahr.mediatork.notification.SilentMissingNotificationHandler
import com.fajrbahr.mediatork.pipeline.AuthorizationPipelineBehavior
import com.fajrbahr.mediatork.pipeline.CachingPipelineBehavior
import com.fajrbahr.mediatork.pipeline.CircuitBreakerPipelineBehavior
import com.fajrbahr.mediatork.pipeline.DeduplicationPipelineBehavior
import com.fajrbahr.mediatork.pipeline.ErrorTrackingPipelineBehavior
import com.fajrbahr.mediatork.pipeline.LoggingPipelineBehavior
import com.fajrbahr.mediatork.pipeline.RateLimitPipelineBehavior
import com.fajrbahr.mediatork.pipeline.RequestCounterPipelineBehavior
import com.fajrbahr.mediatork.pipeline.RetryPipelineBehavior
import com.fajrbahr.mediatork.pipeline.TimeoutPipelineBehavior
import com.fajrbahr.mediatork.pipeline.TimingPipelineBehavior
import com.fajrbahr.mediatork.sample.ktor.after.data.cache.AladhanCacheDataSource
import com.fajrbahr.mediatork.sample.ktor.after.domain.GetIslamicMonthsRequest
import com.fajrbahr.mediatork.sample.ktor.after.domain.GetPrayerTimesRequest
import com.fajrbahr.mediatork.sample.ktor.aftersuper.domain.AfterSuperRegistrar
import com.fajrbahr.mediatork.sample.ktor.aftersuper.domain.GetPrayerTimesValidator
import com.fajrbahr.mediatork.sample.ktor.aftersuper.domain.RequestAuditBehavior
import com.fajrbahr.mediatork.sample.ktor.aftersuper.domain.TraceIdBehavior
import com.fajrbahr.mediatork.sample.ktor.aftersuper.model.AfterSuperIslamicMonthsResponse
import com.fajrbahr.mediatork.sample.ktor.aftersuper.model.AfterSuperPrayerTimesResponse
import com.fajrbahr.mediatork.validator.ValidationBehavior
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureAfterSuperRoutes() {
    val cache = AladhanCacheDataSource()
    val counter = RequestCounterPipelineBehavior(order = 20)

    val mediator = MediatorFactory.create(
        registrars = listOf(AfterSuperRegistrar(cache)),
        pipelineBehaviors = listOf(
            // -200: outermost — wraps each attempt of the retry loop
            RetryPipelineBehavior(maxRetries = 2, delayMillis = 200, order = -200),
            // -100: log entry/exit per attempt
            LoggingPipelineBehavior(logger = { msg -> println("[MediatorK] $msg") }, order = -100),
            // -50: fail fast before any expensive processing
            ValidationBehavior(listOf(GetPrayerTimesValidator()), order = -50),
            // -10: authorization — only fires for AuthenticatedRequest types
            AuthorizationPipelineBehavior(order = -10) { context, request ->
                val traceId = context.getMetaDate<String>("traceId") ?: "no-trace"
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

    routing {
        route("/aftersuper") {
            get("/prayer-times/{city}") {
                val city = call.parameters["city"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "city is required")
                val prayerTimes = mediator.send(GetPrayerTimesRequest(city = city))
                call.respond(
                    AfterSuperPrayerTimesResponse(
                        prayerTimes = prayerTimes,
                        requestCount = counter.countFor(GetPrayerTimesRequest::class),
                    )
                )
            }
            get("/islamic-months") {
                val months = mediator.send(GetIslamicMonthsRequest())
                call.respond(
                    AfterSuperIslamicMonthsResponse(
                        months = months,
                        requestCount = counter.countFor(GetIslamicMonthsRequest::class),
                    )
                )
            }
        }
    }
}
