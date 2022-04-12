package me.lizhen

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.lizhen.service.DgraphService
import me.lizhen.plugins.*
import me.lizhen.schema.CompositePattern
import me.lizhen.schema.Pattern
import me.lizhen.service.MongoService
import me.lizhen.solvers.IdeographContext
import me.lizhen.solvers.PatternSolutionResponse
import me.lizhen.solvers.solveCompositePattern
import me.lizhen.solvers.solvePatternBatched
import kotlin.time.ExperimentalTime
import kotlin.time.TimeMark
import kotlin.time.measureTimedValue

@OptIn(ExperimentalTime::class)
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
                val (result, time) = measureTimedValue {
                    context.solvePatternBatched(pattern)
                }

                call.respond(
                    PatternSolutionResponse(
                        result, time.inWholeMilliseconds, null
                    )
                )
            }

            post("/solveCompositePattern") {
                val pattern = call.receive<CompositePattern>()
                val (result, time) = measureTimedValue {
                    context.solveCompositePattern(pattern)
                }
                call.respond(
                    PatternSolutionResponse(
                        result, time.inWholeMilliseconds, null
                    )
                )
            }
        }

        configureHTTP()
    }.start(wait = true)
}
