package me.lizhen.driver

import io.dgraph.DgraphClient
import io.dgraph.DgraphGrpc
import io.dgraph.DgraphProto
import io.grpc.ManagedChannelBuilder

class IdeographDgraphClient(
    private val hostAddr: String = "192.168.249.10",
    private val port: Int = 19482
){
    private val channel = ManagedChannelBuilder.forAddress(hostAddr, port).usePlaintext().build()
    private val stub = DgraphGrpc.newStub(channel)
    private val dgraphClient = DgraphClient(stub)
    init {
//        println("INIT")
////
////        println(dgraphClient.)
//        val stringBuilder = StringBuilder()
//        stringBuilder.append("<nodeId>: int @index(int) .\n")
//        stringBuilder.append("<types>: string @index(hash) .\n")
//
//        val query = dgraphClient.newTransaction().query("query{node(func: uid(0x1)){}}")
//        println("INITed")
//        println(query.json)
    }
}