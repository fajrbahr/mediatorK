package com.fajrbahr.mediatork.sample.ktor.aftersuper.routes

import com.fajrbahr.mediatork.MediatorFactory
import com.fajrbahr.mediatork.pipeline.ErrorTrackingPipelineBehavior
import com.fajrbahr.mediatork.pipeline.LoggingPipelineBehavior
import com.fajrbahr.mediatork.pipeline.RequestCounterPipelineBehavior
import com.fajrbahr.mediatork.pipeline.RetryPipelineBehavior
import com.fajrbahr.mediatork.pipeline.TimeoutPipelineBehavior
import com.fajrbahr.mediatork.pipeline.TimingPipelineBehavior
import com.fajrbahr.mediatork.sample.ktor.after.data.cache.AladhanCacheDataSource
import com.fajrbahr.mediatork.sample.ktor.after.domain.AppRegistrar
import com.fajrbahr.mediatork.sample.ktor.after.domain.GetIslamicMonthsRequest
import com.fajrbahr.mediatork.sample.ktor.after.domain.GetPrayerTimesRequest
import com.fajrbahr.mediatork.sample.ktor.aftersuper.domain.GetPrayerTimesValidator
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
        registrars = listOf(AppRegistrar(cache)),
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
