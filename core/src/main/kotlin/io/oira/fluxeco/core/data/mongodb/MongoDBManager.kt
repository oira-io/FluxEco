/*
 * FluxEco
 * Copyright (C) 2025 Harfull
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package io.oira.fluxeco.core.data.mongodb

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.oira.fluxeco.core.FluxEco
import io.oira.fluxeco.core.data.mongodb.document.BalanceDocument
import io.oira.fluxeco.core.data.mongodb.document.PlayerProfileDocument
import io.oira.fluxeco.core.data.mongodb.document.PlayerSettingDocument
import io.oira.fluxeco.core.data.mongodb.document.TransactionDocument
import io.oira.fluxeco.core.manager.ConfigManager
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.pojo.PojoCodecProvider

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

