package me.lizhen.plugins

import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.plugins.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import me.lizhen.schema.Pattern
import me.lizhen.schema.PatternNode
import me.lizhen.solvers.IdeographContext
import org.litote.kmongo.MongoOperator
import org.litote.kmongo.json


fun Application.configureRouting() {
    install(AutoHeadResponse)
}
