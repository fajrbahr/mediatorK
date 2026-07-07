package com.fajrbahr.mediatork.sample.ktor

import com.fajrbahr.mediatork.MediatorFactory
import com.fajrbahr.mediatork.sample.ktor.islamicmonths.GetIslamicMonthsQuery
import com.fajrbahr.mediatork.sample.ktor.islamicmonths.IslamicMonthsRegistrar
import com.fajrbahr.mediatork.sample.ktor.prayertimes.GetPrayerTimesQuery
import com.fajrbahr.mediatork.sample.ktor.prayertimes.PrayerTimesRegistrar
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) { json() }

    val cache = AladhanCache()
    val mediator = MediatorFactory.create(
        registrars = listOf(
            PrayerTimesRegistrar(cache),
            IslamicMonthsRegistrar(cache),
        ),
    )

    routing {
        get("/prayer-times/{city}") {
            val city = call.parameters["city"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "city is required")
            call.respond(mediator.send(GetPrayerTimesQuery(city = city)))
        }
        get("/islamic-months") {
            call.respond(mediator.send(GetIslamicMonthsQuery()))
        }
    }
}
