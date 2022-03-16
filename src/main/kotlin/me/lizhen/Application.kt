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


        val mongoClient = IdeographMongoClient()

        val dgraphClient = IdeographDgraphClient()

        mongoClient.test()

        dgraphClient.test()

    }.start(wait = true)
}
