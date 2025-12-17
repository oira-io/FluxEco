package io.oira.fluxeco.core.command.permissions

import io.oira.fluxeco.core.manager.ConfigManager
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin
import revxrsal.commands.Lamp
import revxrsal.commands.annotation.list.AnnotationList
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.command.CommandPermission

class ConfigPermissionFactory(
    private val plugin: JavaPlugin
) : CommandPermission.Factory<BukkitCommandActor> {

    private val configs = mutableMapOf<String, ConfigManager>()

    private fun getConfig(file: String): FileConfiguration {
        return configs.getOrPut(file) {
            ConfigManager(plugin, file)
        }.getConfig()
    }

    override fun create(
        annotations: AnnotationList,
        lamp: Lamp<BukkitCommandActor>
    ): CommandPermission<BukkitCommandActor>? {

        val annotation = annotations.get(ConfigPermission::class.java) ?: return null
        val config = getConfig(annotation.file)

        val permission = config.getString(annotation.key) ?: return null

        return CommandPermission { actor ->
            actor.sender().hasPermission(permission)
        }
    }
}
