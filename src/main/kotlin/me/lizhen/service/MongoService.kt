package me.lizhen.service

import com.mongodb.*
import org.litote.kmongo.*

data class DriverNode(
    val name: String,
    val nodeId: Long
)

class MongoService(
    private val hostAddr: String = "162.105.88.139",
    private val port: Int = 27025,
    private val userName: String = "rootxyx",
    private val password: String = "woxnsk!",
    private val databaseName: String = "relation"
) {
    private val settings = createMongoClientSetting(userName, password, hostAddr, port)
    val client = KMongo.createClient(settings)
    val database = client.getDatabase(databaseName)

    fun test() {

        val collection = database.getCollection<DriverNode>()
    }

    inline fun <reified T : Any>getCollection(name: String) = database.getCollection<T>(name)

    companion object {
        private fun createMongoClientSetting(
            userName: String,
            password: String,
            hostAddr: String,
            port: Int
        ) = MongoClientSettings.builder()
            .apply {
                applyConnectionString(
                    ConnectionString("mongodb://${userName}:${password}@${hostAddr}:${port}")
                )
                credential(
                    MongoCredential.createCredential(userName, "relation", password.toCharArray())
                )
            }
            .build()
    }
}