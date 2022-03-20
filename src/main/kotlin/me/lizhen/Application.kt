package me.lizhen

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.lizhen.service.DgraphService
import me.lizhen.plugins.*
import me.lizhen.schema.Pattern
import me.lizhen.service.MongoService
import me.lizhen.solvers.IdeographContext

fun main() {
    embeddedServer(Netty, port = 8080, host = "localhost") {
        val mongoService = MongoService()
        val dgraphService = DgraphService(mongoService)
        val context = IdeographContext(mongoService, dgraphService)

        configureRouting()
        configureSerialization()

        routing {
            get("/schema") {
                call.respond(context.schema)
            }

            post("/solvePattern") {
                val pattern = call.receive<Pattern>()
                call.respond(context.solvePattern(pattern))
            }
        }

        configureHTTP()
    }.start(wait = true)
}
