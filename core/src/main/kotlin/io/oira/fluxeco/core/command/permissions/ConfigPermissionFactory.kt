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
