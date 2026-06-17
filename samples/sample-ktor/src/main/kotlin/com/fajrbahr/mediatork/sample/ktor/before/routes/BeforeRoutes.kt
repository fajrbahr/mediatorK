package com.fajrbahr.mediatork.sample.ktor.before.routes

import com.fajrbahr.mediatork.sample.ktor.before.data.cache.AladhanCacheDataSource
import com.fajrbahr.mediatork.sample.ktor.before.data.remote.AladhanRemoteDataSource
import com.fajrbahr.mediatork.sample.ktor.before.data.repository.AladhanRepository
import com.fajrbahr.mediatork.sample.ktor.before.domain.GetIslamicMonthsUseCase
import com.fajrbahr.mediatork.sample.ktor.before.domain.GetPrayerTimesUseCase
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureBeforeRoutes() {
    val remote = AladhanRemoteDataSource()
    val cache = AladhanCacheDataSource()
    val repository = AladhanRepository(remote, cache)
    val getPrayerTimes = GetPrayerTimesUseCase(repository)
    val getIslamicMonths = GetIslamicMonthsUseCase(repository)

    routing {
        route("/before") {
            get("/prayer-times/{city}") {
                val city = call.parameters["city"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "city is required")
                call.respond(getPrayerTimes(city))
            }
            get("/islamic-months") {
                call.respond(getIslamicMonths())
            }
        }
    }
}
