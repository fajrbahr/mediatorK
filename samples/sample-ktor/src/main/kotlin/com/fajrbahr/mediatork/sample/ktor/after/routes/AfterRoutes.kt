package com.fajrbahr.mediatork.sample.ktor.after.routes

import com.fajrbahr.mediatork.MediatorFactory
import com.fajrbahr.mediatork.sample.ktor.after.data.cache.AladhanCacheDataSource
import com.fajrbahr.mediatork.sample.ktor.after.domain.AppRegistrar
import com.fajrbahr.mediatork.sample.ktor.after.domain.GetIslamicMonthsRequest
import com.fajrbahr.mediatork.sample.ktor.after.domain.GetPrayerTimesRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureAfterRoutes() {
    val cache = AladhanCacheDataSource()
    val mediator = MediatorFactory.create(registrars = listOf(AppRegistrar(cache)))

    routing {
        route("/after") {
            get("/prayer-times/{city}") {
                val city = call.parameters["city"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "city is required")
                call.respond(mediator.send(GetPrayerTimesRequest(city = city)))
            }
            get("/islamic-months") {
                call.respond(mediator.send(GetIslamicMonthsRequest()))
            }
        }
    }
}
