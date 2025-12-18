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

package io.oira.fluxeco.core.data

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.oira.fluxeco.core.FluxEco
import io.oira.fluxeco.core.data.mongodb.MongoDBManager
import io.oira.fluxeco.core.data.table.Balances
import io.oira.fluxeco.core.data.table.PlayerProfiles
import io.oira.fluxeco.core.data.table.PlayerSettings
import io.oira.fluxeco.core.data.table.Transactions
import io.oira.fluxeco.core.manager.ConfigManager
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseManager {
    private lateinit var dataSource: HikariDataSource
    private lateinit var database: Database
    private val plugin: FluxEco = FluxEco.instance
    private val cfg = ConfigManager(plugin, "database.yml").getConfig()
    private var initialized: Boolean = false
    private var dbType: String = "sqlite"
    private var isMongoDb: Boolean = false

    fun init() {
        dbType = cfg.getString("database.type", "sqlite")!!.lowercase()

        if (dbType == "mongodb") {
            isMongoDb = true
            try {
                MongoDBManager.init()
                initialized = true
                plugin.logger.info("MongoDB initialized successfully!")
            } catch (e: Exception) {
                initialized = false
                plugin.logger.severe("Failed to initialize MongoDB: ${e.message}")
                throw e
            }
            return
        }

        isMongoDb = false
        val mysqlUri = cfg.getString("database.mysql.uri")?.takeIf { it.isNotBlank() }

        val jdbcUrl = when {
            mysqlUri != null -> mysqlUri
            dbType == "sqlite" -> "jdbc:sqlite:${plugin.dataFolder}/data.db"
            dbType == "mysql" -> {
                val host = cfg.getString("database.mysql.host", "localhost")
                val port = cfg.getInt("database.mysql.port", 3306)
                val dbName = cfg.getString("database.mysql.database", "fluxeco")
                "jdbc:mysql://$host:$port/$dbName?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
            }
            dbType == "h2" -> "jdbc:h2:file:${plugin.dataFolder}/data;AUTO_SERVER=TRUE"
            else -> throw IllegalArgumentException("Unsupported database type: $dbType")
        }

        plugin.logger.info("Attempting to connect to database: $dbType")
        if (dbType == "mysql") {
            plugin.logger.info("MySQL connection string: ${jdbcUrl.replace(Regex("password=[^&]*"), "password=****")}")
        }

        val hikariConfig = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            if (dbType == "mysql") {
                username = cfg.getString("database.mysql.username", "root")
                password = cfg.getString("database.mysql.password", "")
            }
            maximumPoolSize = cfg.getInt("database.mysql.poolSize", 10)
            isAutoCommit = false
            connectionTimeout = 30000
            validationTimeout = 5000

            driverClassName = when (dbType) {
                "sqlite" -> "org.sqlite.JDBC"
                "mysql" -> "com.mysql.cj.jdbc.Driver"
                "h2" -> "org.h2.Driver"
                else -> null
            }
        }

        try {
            dataSource = HikariDataSource(hikariConfig)
            database = Database.connect(dataSource)

            transaction(database) {
                SchemaUtils.create(Balances, PlayerProfiles, Transactions, PlayerSettings)
            }

            initialized = true
            plugin.logger.info("Database connection established successfully!")
        } catch (e: Exception) {
            initialized = false
            plugin.logger.severe("Failed to initialize database: ${e.message}")
            throw e
        }
    }

    fun getDatabase(): Database {
        if (!initialized) {
            throw IllegalStateException("Database not initialized")
        }
        if (isMongoDb) {
            throw IllegalStateException("Cannot get SQL Database when MongoDB is configured. Use MongoDBManager instead.")
        }
        return database
    }

    fun isMongoDB(): Boolean = isMongoDb

    fun getDatabaseType(): String = dbType

    fun shutdown() {
        if (!initialized) {
            plugin.logger.warning("Database was not initialized, nothing to close")
            return
        }

        try {
            if (isMongoDb) {
                MongoDBManager.shutdown()
            } else if (!dataSource.isClosed) {
                dataSource.close()
                plugin.logger.info("Database connection closed")
            }
        } catch (e: Exception) {
            plugin.logger.warning("Error closing database connection: ${e.message}")
        } finally {
            initialized = false
        }
    }
}

