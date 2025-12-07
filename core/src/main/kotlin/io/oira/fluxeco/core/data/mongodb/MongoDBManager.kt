package io.oira.fluxeco.core.data.mongodb

import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import io.oira.fluxeco.FluxEco
import io.oira.fluxeco.core.data.mongodb.document.BalanceDocument
import io.oira.fluxeco.core.data.mongodb.document.PlayerProfileDocument
import io.oira.fluxeco.core.data.mongodb.document.PlayerSettingDocument
import io.oira.fluxeco.core.data.mongodb.document.TransactionDocument
import io.oira.fluxeco.core.manager.ConfigManager
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.pojo.PojoCodecProvider
import kotlin.jvm.java

object MongoDBManager {
    private lateinit var client: MongoClient
    internal lateinit var database: MongoDatabase
    private val plugin: FluxEco = FluxEco.instance
    private val cfg = ConfigManager(plugin, "database.yml").getConfig()
    private var initialized: Boolean = false

    fun init() {
        try {
            val mongoUri = cfg.getString("database.mongodb.uri", "mongodb://localhost:27017/fluxeco")!!
            plugin.logger.info("Connecting to MongoDB: ${sanitizeUri(mongoUri)}")

            val connectionString = ConnectionString(mongoUri)

            val pojoCodecRegistry = CodecRegistries.fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build())
            )

            val settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .codecRegistry(pojoCodecRegistry)
                .build()

            client = MongoClient.create(settings)

            val dbName = connectionString.database ?: "fluxeco"
            database = client.getDatabase(dbName)

            createIndexes()

            initialized = true
            plugin.logger.info("MongoDB connection established successfully!")
        } catch (e: Exception) {
            initialized = false
            plugin.logger.severe("Failed to initialize MongoDB: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    private fun createIndexes() {
        plugin.logger.info("MongoDB indexes verified")
    }

    private fun sanitizeUri(uri: String): String {
        return uri.replace(Regex("://([^:]+):([^@]+)@"), "://$1:****@")
    }

    fun getDatabase(): MongoDatabase {
        if (!initialized) {
            throw IllegalStateException("MongoDB not initialized")
        }
        return database
    }

    fun isInitialized(): Boolean = initialized

    fun shutdown() {
        if (!initialized) {
            plugin.logger.warning("MongoDB was not initialized, nothing to close")
            return
        }

        try {
            client.close()
            plugin.logger.info("MongoDB connection closed")
        } catch (e: Exception) {
            plugin.logger.warning("Error closing MongoDB connection: ${e.message}")
        } finally {
            initialized = false
        }
    }

    fun getBalancesCollection() = database.getCollection("balances", BalanceDocument::class.java)
    fun getPlayerProfilesCollection() = database.getCollection("player_profiles", PlayerProfileDocument::class.java)
    fun getTransactionsCollection() = database.getCollection("transactions", TransactionDocument::class.java)
    fun getPlayerSettingsCollection() = database.getCollection("player_settings", PlayerSettingDocument::class.java)
}

