package me.lizhen.service

import com.mongodb.*
import org.litote.kmongo.reactivestreams.*
import org.litote.kmongo.coroutine.*

class MongoService(
    hostAddress: String = "162.105.88.139",
    port: Int = 27025,
    userName: String = "rootxyx",
    password: String = "woxnsk!",
    databaseName: String = "relation"
) {
    private val settings = createMongoClientSetting(userName, password, hostAddress, port)
    public val client = KMongo.createClient(settings).coroutine
    public val database = client.getDatabase(databaseName)


    inline fun <reified T : Any>getCollection(name: String) = database.getCollection<T>(name)

    companion object {
        private fun createMongoClientSetting(
            userName: String,
            password: String,
            hostAddress: String,
            port: Int
        ) = MongoClientSettings.builder()
            .apply {
                applyConnectionString(
                    ConnectionString("mongodb://${userName}:${password}@${hostAddress}:${port}")
                )
                credential(
                    MongoCredential.createCredential(userName, "relation", password.toCharArray())
                )
            }
            .build()
    }
}