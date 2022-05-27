package me.lizhen

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.lizhen.plugins.*
import me.lizhen.schema.CompositePattern
import me.lizhen.schema.ConceptNode
import me.lizhen.schema.HasRelationConceptEdge
import me.lizhen.schema.Pattern
import me.lizhen.service.MongoService
import me.lizhen.solvers.*
import me.lizhen.utils.withTimeMeasure
import sun.security.krb5.internal.HostAddress
import java.time.Month

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)


//fun addFakeEdge(schema: IdeographSchema): IdeographSchema {
//    val edges = schema.hasRelationConceptEdges
//    val newEdges = edges.flatMap { e ->
//        (0..3).map { index ->
//            HasRelationConceptEdge(
//                e._id + index,
//                e.fromId,
//                e.toId,
//                e.edgeId + index,
//                e.relationId + index,
//                e.name + index,
//            )
//        }
//    }
//    return schema.copy(
//        hasRelationConceptEdges = newEdges
//    )
//}


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

    val mongoPort = environment.config.propertyOrNull("ktor.ideograph.mongodb.port")?.getString()
    val mongoHost = environment.config.propertyOrNull("ktor.ideograph.mongodb.host")?.getString()
    val mongoUserName = environment.config.propertyOrNull("ktor.ideograph.mongodb.userName")?.getString()
    val mongoPassword = environment.config.propertyOrNull("ktor.ideograph.mongodb.password")?.getString()
    val mongoDatabaseName = environment.config.propertyOrNull("ktor.ideograph.mongodb.databaseName")?.getString()

    val mongoService = MongoService(
        mongoPort?.toInt() ?: 27025,
        mongoHost ?: "162.105.88.139",
        mongoUserName ?: "rootxyx",
        mongoPassword ?: "woxnsk!",
        mongoDatabaseName ?: "relation"
    )

    val contextMap = mutableMapOf<String, IdeographContext>();


    // val dgraphService = DgraphService(mongoService)
    val context = IdeographContext(mongoService, true)

    configureRouting()
    configureSerialization()

    routing {

        post("/connectDatabase") {
            val oath = call.receive<IdeographDatabaseOath>()
            val newMongoService = MongoService(
                oath.mongoPort,
                oath.hostAddress ?: "162.105.88.139",
                oath.mongoUserName ?: "rootxyx",
                oath.mongoPassWord ?: "woxnsk!",
                oath.mongoPassWord ?: "relation"
            )
            val newContext = IdeographContext(newMongoService)
            contextMap[oath.identifier] = newContext
        }

        post("/getSchemaFromDatabase") {
            val contextIdentifier = call.receive<String>()
            val schema = contextMap[contextIdentifier]
            if (schema != null) {
                call.respond(schema)
            } else {
                call.respond(Error("Unable to resolve identifier."))
            }
        }

        post("/solveCompositePatternFromDatabase") {
            val data = call.receive<IdeographCompositePattern>()
            val selectedContext = contextMap[data.databaseIdentifier]
            if (selectedContext == null) {
                call.respond(Error("Unable to resolve identifier"))
            }
            else {
                val (time, result) = withTimeMeasure {
                    selectedContext.solveCompositePattern(data.pattern)
                }
                call.respond(
                    PatternSolutionResponse(
                        result, time, null
                    )
                )
            }
        }

        get("/schema") {
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
    }

    configureHTTP()
//    }.start(wait = true)
}
