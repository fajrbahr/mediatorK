package com.fajrbahr.mediatork.sample.ktor

import com.fajrbahr.mediatork.sample.ktor.after.routes.configureAfterRoutes
import com.fajrbahr.mediatork.sample.ktor.aftersuper.routes.configureAfterSuperRoutes
import com.fajrbahr.mediatork.sample.ktor.before.routes.configureBeforeRoutes
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) { json() }
    configureBeforeRoutes()
    configureAfterRoutes()
    configureAfterSuperRoutes()
}
