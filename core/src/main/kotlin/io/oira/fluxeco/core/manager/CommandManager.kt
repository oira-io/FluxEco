package io.oira.fluxeco.core.manager

import io.oira.fluxeco.core.FluxEco
import io.oira.fluxeco.core.command.*
import io.oira.fluxeco.core.command.permissions.ConfigPermissionFactory
import io.oira.fluxeco.core.lamp.AsyncOfflinePlayer
import io.oira.fluxeco.core.redis.RedisManager
import org.bukkit.Bukkit
import revxrsal.commands.bukkit.BukkitLamp
import revxrsal.commands.orphan.OrphanCommand
import revxrsal.commands.orphan.Orphans
import kotlin.reflect.KClass

object CommandManager {

    private val plugin: FluxEco = FluxEco.instance
    private val config = ConfigManager(plugin, "commands.yml").getConfig()

    private val commandMap: Map<String, KClass<out OrphanCommand>> = mapOf(
        "pay" to PayCommand::class,
        "economy" to EcoCommand::class,
        "balance" to BalanceCommand::class,
        "balance-top" to BaltopCommand::class,
        "transaction-history" to HistoryCommand::class,
        "pay-alerts" to PayAlertsCommand::class,
        "pay-toggle" to PayToggleCommand::class,
        "stats" to StatsCommand::class
    )

    /**
     * Initializes the command framework and registers Orphan-based command handlers declared in `commandMap`.
     *
     * Builds and configures the command lamp, then iterates `commandMap` to instantiate and register each command
     * whose configuration section "commands.<name>" exists, has `enabled` true (default true), and provides at least one alias.
     *
     * The suggestion provider for `AsyncOfflinePlayer` supplies online player names that are not marked as vanished
     * and, when Redis is enabled, adds player names from the Redis cache.
     *
     * Any exception during initialization or registration is logged as a severe error and the stack trace is printed.
     */
    fun register() {
        try {
            val lamp = BukkitLamp.builder(plugin)
                .permissionFactory(ConfigPermissionFactory(plugin))
                .parameterTypes { types ->
                    types.addParameterType(
                        AsyncOfflinePlayer::class.java,
                        AsyncOfflinePlayer.parameterType()
                    )
                }
                .suggestionProviders { suggestions ->
                    suggestions.addProvider(AsyncOfflinePlayer::class.java) {
                        val names = mutableSetOf<String>()

                        Bukkit.getOnlinePlayers()
                            .filter {
                                !it.hasMetadata("vanished") ||
                                        it.getMetadata("vanished").all { meta -> !meta.asBoolean() }
                            }
                            .forEach { names.add(it.name) }

                        if (RedisManager.isEnabled) {
                            RedisManager.getCache()
                                ?.getAllPlayerNames()
                                ?.let(names::addAll)
                        }

                        names
                    }
                }
                .build()

            commandMap.forEach { (key, clazz) ->
                val section = config.getConfigurationSection("commands.$key") ?: return@forEach
                if (!section.getBoolean("enabled", true)) return@forEach

                val aliases = section.getStringList("aliases")
                if (aliases.isEmpty()) return@forEach

                val command = clazz.constructors.first().call()

                lamp.register(
                    Orphans
                        .path(*aliases.toTypedArray())
                        .handler(command)
                )
            }

        } catch (e: Exception) {
            plugin.logger.severe("Failed to register commands: ${e.message}")
            e.printStackTrace()
        }
    }
}