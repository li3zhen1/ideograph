package me.lizhen.service

import io.dgraph.DgraphClient
import io.dgraph.DgraphGrpc
import io.dgraph.DgraphProto
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.*
import me.lizhen.schema.PatternNode
import me.lizhen.schema.RelationNode

class DgraphService(
    private val mongoService: MongoService,
    private val hostAddr: String = "162.105.88.139",
    private val port: Int = 19482,
) {
    private val channel = ManagedChannelBuilder.forAddress(hostAddr, port).usePlaintext().build()
    private val stub = DgraphGrpc.newStub(channel)
    public val client = DgraphClient(stub)

    init {
        CoroutineScope(Dispatchers.IO).launch {
            setDgraphSchema()
            client.newTransaction().query("query{node(func: uid(0x1)){}}")
        }
    }


    private suspend fun setDgraphSchema() {
        val stringBuilder = StringBuilder()
        stringBuilder.append("<nodeId>: int @index(int) .\n")
        stringBuilder.append("<types>: string @index(hash) .\n")

        mongoService
            .getCollection<RelationNode>("relation_node")
            .find()
            .consumeEach {
                stringBuilder.append("<${it.name}>: [uid] @reverse .\n")
            }
//            .forEach {
//                stringBuilder.append("<${it.name}>: [uid] @reverse .\n")
//            }

        val operation = DgraphProto.Operation.newBuilder()
            .setSchema(stringBuilder.toString())
            .setRunInBackground(true)
            .build()
        client.alter(operation)
    }

}