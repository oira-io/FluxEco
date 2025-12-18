package io.oira.fluxeco.core

import com.tcoded.folialib.FoliaLib
import io.oira.fluxeco.api.IFluxEcoAPI
import io.oira.fluxeco.core.api.EconomyManagerImpl
import io.oira.fluxeco.core.api.FluxEcoAPIImpl
import io.oira.fluxeco.core.api.TransactionManagerImpl
import io.oira.fluxeco.core.cache.CacheManager
import io.oira.fluxeco.core.data.DatabaseManager
import io.oira.fluxeco.core.gui.impl.BaltopGUI
import io.oira.fluxeco.core.gui.impl.ConfirmPaymentGUI
import io.oira.fluxeco.core.gui.impl.HistoryGUI
import io.oira.fluxeco.core.gui.impl.StatsGUI
import io.oira.fluxeco.core.integration.Metrics
import io.oira.fluxeco.core.integration.MiniPlaceholders
import io.oira.fluxeco.core.integration.PlaceholderAPI
import io.oira.fluxeco.core.integration.Vault
import io.oira.fluxeco.core.listener.PlayerJoinListener
import io.oira.fluxeco.core.listener.PlayerQuitListener
import io.oira.fluxeco.core.manager.CommandManager
import io.oira.fluxeco.core.manager.ConfigManager
import io.oira.fluxeco.core.manager.EconomyManager
import io.oira.fluxeco.core.manager.MessageManager
import io.oira.fluxeco.core.redis.RedisManager
import io.oira.fluxeco.core.util.DateFormatter
import io.oira.fluxeco.core.util.NumberFormatter
import io.oira.fluxeco.core.util.Placeholders
import io.oira.fluxeco.core.util.Threads
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.event.HandlerList
import org.bukkit.plugin.ServicePriority
import org.bukkit.plugin.java.JavaPlugin

class FluxEco : JavaPlugin() {

    companion object {
        lateinit var instance: FluxEco
            private set
    }

    init {
        instance = this
    }

    val foliaLib = FoliaLib(this)

    private var baltopGuiInstance: BaltopGUI? = null
    val baltopGui: BaltopGUI
        get() = baltopGuiInstance ?: throw IllegalStateException("BaltopGUI not initialized")

    private var historyGuiInstance: HistoryGUI? = null
    val historyGui: HistoryGUI
        get() = historyGuiInstance ?: throw IllegalStateException("HistoryGUI not initialized")

    private var confirmPaymentGuiInstance: ConfirmPaymentGUI? = null
    val confirmPaymentGui: ConfirmPaymentGUI
        get() = confirmPaymentGuiInstance ?: throw IllegalStateException("ConfirmPaymentGUI not initialized")

    private var statsGuiInstance: StatsGUI? = null
    val statsGui: StatsGUI
        get() = statsGuiInstance ?: throw IllegalStateException("StatsGUI not initialized")

    val pluginId: Int = 27752

    /**
     * Initializes and starts the FluxEco plugin by loading configuration, formatters,
     * threads, persistence and cache layers, API and integrations, GUIs, listeners,
     * commands, and prints the startup banner.
     *
     * Initializes default configuration, the ConfigManager, number and date formatters,
     * database, Redis, cache, the plugin API, metrics, GUIs, Vault and placeholder
     * integrations, registers event listeners and commands, and displays the startup banner.
     * If an exception occurs during startup, the error is logged, the stack trace is printed,
     * and the plugin is disabled.
     */
    override fun onEnable() {
        try {
            saveDefaultConfig()

            val configManager = ConfigManager(this, "config.yml")
            NumberFormatter.init(configManager)
            DateFormatter.init(this, configManager)

            Threads.load()

            initializeDatabase()

            initializeRedis()

            initializeCache()

            initializeAPI(configManager)

            Metrics(this, pluginId)

            initializeGUIs()

            initializeVault()

            initializePlaceholderAPI()

            initializeMiniPlaceholders()

            registerListeners()

            CommandManager.register()

            displayStartupBanner()
        } catch (e: Exception) {
            logger.severe("Failed to enable FluxEco: ${e.message}")
            e.printStackTrace()
            server.pluginManager.disablePlugin(this)
        }
    }

    /**
     * Performs an orderly shutdown of the plugin, releasing resources and unregistering components.
     *
     * Cleans up GUI instances, unsets the public API instance, shuts down cache, Redis, database, and thread helpers,
     * and logs successful disablement. If an error occurs during shutdown, a warning is logged and the stack trace is printed.
     */
    override fun onDisable() {
        try {
            cleanupGUIs()

            IFluxEcoAPI.unsetInstance()

            CacheManager.shutdown()

            RedisManager.shutdown()

            DatabaseManager.shutdown()

            Threads.close()

            logger.info("FluxEco has been disabled successfully!")
        } catch (e: Exception) {
            logger.warning("Error during plugin shutdown: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun initializeDatabase() {
        try {
            DatabaseManager.init()
        } catch (e: Exception) {
            logger.severe("Failed to initialize database: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Initializes the Redis subsystem for the plugin.
     *
     * Attempts to start Redis via RedisManager; on failure it logs a warning with the error message and continues startup.
     */
    private fun initializeRedis() {
        try {
            RedisManager.init()
        } catch (e: Exception) {
            logger.warning("Failed to initialize Redis: ${e.message}")
        }
    }

    /**
     * Initializes the plugin's cache subsystem.
     *
     * Logs an informational message on successful initialization or a warning if initialization fails.
     */
    private fun initializeCache() {
        try {
            CacheManager.init()
            logger.info("Cache system initialized successfully!")
        } catch (e: Exception) {
            logger.warning("Failed to initialize cache: ${e.message}")
        }
    }

    /**
     * Initializes and registers the FluxEco API implementation for the plugin.
     *
     * Creates the economy and transaction managers, constructs the API with the plugin version,
     * and sets it as the global IFluxEcoAPI instance.
     *
     * @param configManager Provides configuration needed to construct the economy manager.
     * @throws Exception If any error occurs while initializing or registering the API.
     */
    private fun initializeAPI(configManager: ConfigManager) {
        try {
            val economyManager = EconomyManagerImpl(this, configManager)
            val transactionManager = TransactionManagerImpl()

            @Suppress("DEPRECATION")
            val api = FluxEcoAPIImpl(
                economyManager = economyManager,
                transactionManager = transactionManager,
                version = description.version
            )

            IFluxEcoAPI.setInstance(api)
            logger.info("FluxEco API initialized successfully!")
        } catch (e: Exception) {
            logger.severe("Failed to initialize API: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Registers the plugin's player join and player quit event listeners with the server plugin manager.
     */
    private fun registerListeners() {
        server.pluginManager.registerEvents(PlayerJoinListener(), this)
        server.pluginManager.registerEvents(PlayerQuitListener(), this)
    }

    /**
     * Initializes GUI instances and registers them as event listeners.
     *
     * Creates and stores instances for Baltop, History, and ConfirmPayment GUIs and,
     * if "stats.enabled" is true in the plugin config, for the Stats GUI, then
     * registers each with the server's plugin manager.
     *
     * @throws Exception if any GUI fails to initialize or register; the exception is rethrown.
     */
    private fun initializeGUIs() {
        try {
            baltopGuiInstance = BaltopGUI()
            server.pluginManager.registerEvents(baltopGuiInstance!!, this)

            historyGuiInstance = HistoryGUI()
            server.pluginManager.registerEvents(historyGuiInstance!!, this)

            confirmPaymentGuiInstance = ConfirmPaymentGUI()
            server.pluginManager.registerEvents(confirmPaymentGuiInstance!!, this)

            if (config.getBoolean("stats.enabled", true)) {
                statsGuiInstance = StatsGUI()
                server.pluginManager.registerEvents(statsGuiInstance!!, this)
            }
        } catch (e: Exception) {
            logger.severe("Failed to initialize GUIs: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    private fun cleanupGUIs() {
        try {
            baltopGuiInstance?.closeAll()
            baltopGuiInstance?.let { HandlerList.unregisterAll(it) }

            historyGuiInstance?.closeAll()
            historyGuiInstance?.let { HandlerList.unregisterAll(it) }

            confirmPaymentGuiInstance?.closeAll()
            confirmPaymentGuiInstance?.let { HandlerList.unregisterAll(it) }

            statsGuiInstance?.closeAll()
            statsGuiInstance?.let { HandlerList.unregisterAll(it) }

            logger.info("Cleaned up GUIs successfully")
        } catch (e: Exception) {
            logger.warning("Error cleaning up GUIs: ${e.message}")
        }
    }

    private fun initializeVault() {
        if (server.pluginManager.getPlugin("Vault") == null) {
            return
        }

        try {
            val vault = Vault(this, EconomyManager)
            server.servicesManager.register(
                Economy::class.java,
                vault,
                this,
                ServicePriority.Normal
            )
        } catch (e: Exception) {
            logger.warning("Failed to initialize Vault integration: ${e.message}")
        }
    }

    private fun initializePlaceholderAPI() {
        if (server.pluginManager.getPlugin("PlaceholderAPI") == null) {
            return
        }

        try {
            PlaceholderAPI().register()
        } catch (e: Exception) {
            logger.warning("Failed to initialize PlaceholderAPI integration: ${e.message}")
        }
    }

    private fun initializeMiniPlaceholders() {
        if (server.pluginManager.getPlugin("MiniPlaceholders") == null) {
            return
        }

        try {
            MiniPlaceholders().register()
        } catch (e: Exception) {
            logger.warning("Failed to initialize MiniPlaceholders integration: ${e.message}")
        }
    }

    @Suppress("UnstableApiUsage")
    private fun displayStartupBanner() {
        val console = Bukkit.getConsoleSender()
        val manager = MessageManager.getInstance()

        val banner = """
            
        &r &f&#B833FF███████╗██╗     ██╗   ██╗██╗  ██╗███████╗ ██████╗ ██████╗ 
        &r &f&#B833FF██╔════╝██║     ██║   ██║╚██╗██╔╝██╔════╝██╔════╝██╔═══██╗
        &r &f&#B833FF█████╗  ██║     ██║   ██║ ╚███╔╝ █████╗  ██║     ██║   ██║
        &r &f&#B833FF██╔══╝  ██║     ██║   ██║ ██╔██╗ ██╔══╝  ██║     ██║   ██║
        &r &f&#B833FF██║     ███████╗╚██████╔╝██╔╝ ██╗███████╗╚██████╗╚██████╔╝
        &r &f&#B833FF╚═╝     ╚══════╝ ╚═════╝ ╚═╝  ╚═╝╚══════╝ ╚═════╝ ╚═════╝ 
                                                                                    
    """.trimIndent()

        banner.lines().forEach { line ->
            manager.sendMessage(console, line, prefix = false)
        }

        val dbType = try {
            val cfg = ConfigManager(this, "database.yml").getConfig()
            val type = cfg.getString("database.type", "sqlite") ?: "sqlite"
            when (type.lowercase()) {
                "sqlite" -> "SQLite"
                "mysql" -> "MySQL"
                "h2" -> "H2"
                "mongodb" -> "MongoDB"
                else -> type.lowercase().replaceFirstChar { it.uppercase() }
            }
        } catch (_: Exception) {
            "SQLite"
        }

        val placeholders = Placeholders.builder()
            .add("version", pluginMeta.version)
            .add("java_version", System.getProperty("java.version"))
            .add("server_software", "${Bukkit.getName()} ${Bukkit.getVersion()}")
            .add("os", "${System.getProperty("os.name")} ${System.getProperty("os.arch")}")
            .add("db_type", dbType)
            .build()

        val infoLines = listOf(
            "",
            " &#B0B0B0&lInformation:",
            " &8▸ &fVersion: &#B833FF{version}",
            " &8▸ &fDatabase: &#B833FF{db_type}",
            " &8▸ &fServer Software: &#B833FF{server_software}",
            " &8▸ &fJava: &#B833FF{java_version}",
            " &8▸ &fOS: &#B833FF{os}",
            "",
            " &#B833FFFluxEco &fenabled successfully!"
        )

        infoLines.forEach { line ->
            manager.sendMessage(console, line, placeholders, prefix = false)
        }
    }
}