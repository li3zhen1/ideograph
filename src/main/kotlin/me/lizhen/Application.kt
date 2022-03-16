package me.lizhen

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import me.lizhen.driver.IdeographDgraphClient
import me.lizhen.driver.IdeographMongoClient
import me.lizhen.plugins.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "localhost") {
        configureRouting()
        configureSerialization()
        configureHTTP()

//        val dgraphClient = IdeographDgraphClient()
        val mongoClient = IdeographMongoClient()

        mongoClient.test()

    }.start(wait = true)
}
