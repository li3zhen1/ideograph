package me.lizhen.driver

import com.mongodb.*
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClientFactory
import com.mongodb.client.MongoClients
import io.ktor.serialization.kotlinx.json.*
import org.litote.kmongo.*
import java.util.*

data class DriverNode(
    val name: String,
    val nodeId: Long
)

class IdeographMongoClient(
    private val hostAddr: String = "162.105.88.139",
    private val port: Int = 27025,
    private val userName: String = "rootxyx",
    private val password: String = "woxnsk!",
    private val databaseName: String = "relation"
) {
    private val connectionString = "mongodb://${userName}:${password}@${hostAddr}:${port}"
    private val mongoClientSettings = MongoClientSettings.builder()
        .apply {
            applyConnectionString(ConnectionString(connectionString))
            credential(MongoCredential.createCredential(userName, "relation", password.toCharArray()))
        }
        .build()

    private val client = KMongo.createClient(mongoClientSettings)

    init {

    }

    fun test() {

        val database = client.getDatabase(databaseName)

        val lcn = database.listCollectionNames()

        lcn.forEach {
            println(it)
        }

    }

    companion object {
        val mongoClients = MongoClientFactory().apply {

        }
    }
}