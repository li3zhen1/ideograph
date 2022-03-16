package me.lizhen

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import me.lizhen.service.DgraphService
import me.lizhen.plugins.*
import me.lizhen.service.MongoService
import me.lizhen.solvers.IdeographContext

fun main() {
    embeddedServer(Netty, port = 8080, host = "localhost") {
        configureRouting()
        configureSerialization()
        configureHTTP()

        val mongoService = MongoService()
        val dgraphService = DgraphService(mongoService)
        val ideographContext = IdeographContext(mongoService, dgraphService)

//        ideographContext.apply {
//            initializeSchema()
//        }

        mongoService.test()

    }.start(wait = true)
}
