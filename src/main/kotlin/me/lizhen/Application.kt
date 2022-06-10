package me.lizhen

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.lizhen.plugins.*
import me.lizhen.schema.*
import me.lizhen.service.MongoService
import me.lizhen.solvers.*
import me.lizhen.utils.withTimeMeasure

/**
 * @see <a href="https://ktor.io/docs/configurations.html">Ktor 文档</a>
 */
fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)


/**
 * 数据库的配置
 */
data class IdeographDatabaseOath(
    val mongoPort: Int,
    val hostAddress: String?,
    val mongoUserName: String?,
    val mongoPassWord: String?,
    val mongoDatabaseName: String?
) {
    public val identifier get() = "$hostAddress:$mongoPort"
}


data class IdeographCompositePattern(
    val pattern: CompositePattern,
    val databaseIdentifier: String,
)


fun Application.module() {

    /**
     * read from conf
     */
    val mongoPort = environment.config.propertyOrNull("ktor.ideograph.mongodb.port")?.getString()
    val mongoHost = environment.config.propertyOrNull("ktor.ideograph.mongodb.host")?.getString()
    val mongoUserName = environment.config.propertyOrNull("ktor.ideograph.mongodb.userName")?.getString()
    val mongoPassword = environment.config.propertyOrNull("ktor.ideograph.mongodb.password")?.getString()
    val mongoDatabaseName = environment.config.propertyOrNull("ktor.ideograph.mongodb.databaseName")?.getString()

    val mongoService = MongoService(
        mongoPort?.toIntOrNull() ?: 27025,
        mongoHost ?: "162.105.88.139",
        mongoUserName ?: "rootxyx",
        mongoPassword ?: "woxnsk!",
        mongoDatabaseName ?: "relation"
    )

    // val dgraphService = DgraphService(mongoService)
    val context = IdeographContext(mongoService, true)

    configureRouting()
    configureSerialization()


    routing {


        get("/schema") {
//            var nodes = context.schema.conceptNodes.toMutableList()
//            nodes[0] = nodes[0].copy(name = "Person")
//            nodes[1] = nodes[1].copy(name = "Movie")
//            var schema = context.schema.copy(
//                conceptNodes = nodes
//            )
            call.respond(context.schema)
        }

        post("/solvePattern") {
            val pattern = call.receive<Pattern>()
            val (time, result) = withTimeMeasure {
                context.solvePatternBatched(pattern)
            }

            call.respond(
                PatternSolutionResponse(
                    result, time, null
                )
            )
        }

        post("/solveCompositePattern") {
            val pattern = call.receive<CompositePattern>()
            println(pattern)
            val (time, result) = withTimeMeasure {
                context.solveCompositePattern(pattern)
            }
            call.respond(
                PatternSolutionResponse(
                    result, time, null
                )
            )
        }

        post("/solveCompositePatternWithAggregation") {
            val pattern = call.receive<AggregatedPattern>()
            println(pattern)
            val (time, result) = withTimeMeasure {
                context.solveCompositePatternWithAggregation(pattern)
            }
            call.respond(
                AggregatedPatternSolutionResponse(
                    result, time, null
                )
            )
        }
    }

    configureHTTP()
//    }.start(wait = true)
}
