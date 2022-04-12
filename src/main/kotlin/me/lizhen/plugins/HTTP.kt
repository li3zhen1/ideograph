package me.lizhen.plugins

import io.ktor.http.*
import io.ktor.server.plugins.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.request.*

fun Application.configureHTTP() {
    install(CORS) {

        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)

        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)

//        allowCredentials = true

        anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
    }

}
