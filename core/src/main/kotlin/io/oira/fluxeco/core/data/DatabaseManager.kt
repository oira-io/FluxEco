package io.oira.fluxeco.core.data

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.oira.fluxeco.FluxEco
import io.oira.fluxeco.core.data.table.Balances
import io.oira.fluxeco.core.data.table.PlayerProfiles
import io.oira.fluxeco.core.data.table.PlayerSettings
import io.oira.fluxeco.core.data.table.Transactions
import io.oira.fluxeco.core.manager.ConfigManager
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseManager {
    private var dataSource: HikariDataSource? = null
    private var database: Database? = null
    private val plugin: FluxEco = FluxEco.instance
    private val cfg = ConfigManager(plugin, "database.yml").getConfig()

    fun init() {
        val dbType = cfg.getString("database.type", "sqlite")!!.lowercase()
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
            connectionTimeout = 30000 // 30 seconds
            validationTimeout = 5000 // 5 seconds

            driverClassName = when (dbType) {
                "sqlite" -> "org.sqlite.JDBC"
                "mysql" -> "com.mysql.cj.jdbc.Driver"
                "h2" -> "org.h2.Driver"
                else -> null
            }
        }

        try {
            dataSource = HikariDataSource(hikariConfig)
            database = Database.connect(dataSource!!)

            transaction(database) {
                SchemaUtils.create(Balances, PlayerProfiles, Transactions, PlayerSettings)
            }

            plugin.logger.info("Database connection established successfully!")
        } catch (e: Exception) {
            plugin.logger.severe("Failed to initialize database: ${e.message}")
            plugin.logger.severe("Please check:")
            plugin.logger.severe("  1. MySQL server is running")
            plugin.logger.severe("  2. Database '${cfg.getString("database.mysql.database", "fluxeco")}' exists")
            plugin.logger.severe("  3. Credentials in database.yml are correct")
            plugin.logger.severe("  4. MySQL is accessible on ${cfg.getString("database.mysql.host", "localhost")}:${cfg.getInt("database.mysql.port", 3306)}")
            throw e
        }
    }

    fun getDatabase(): Database = database
        ?: throw IllegalStateException("Database not initialized")

    fun shutdown() {
        dataSource?.let {
            if (!it.isClosed) {
                it.close()
                plugin.logger.info("Database connection closed")
            }
        } ?: plugin.logger.warning("Database was not initialized, nothing to close")
    }
}