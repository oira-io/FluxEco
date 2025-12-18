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

package io.oira.fluxeco.core.manager

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.IOException

class ConfigManager(private val plugin: JavaPlugin, private val fileName: String) {

    private val file: File = File(plugin.dataFolder, fileName)
    private var config: FileConfiguration

    init {
        ensureFileExists()
        config = YamlConfiguration.loadConfiguration(file)
        instances.add(this)
    }

    private fun ensureFileExists() {
        if (!file.exists()) {
            plugin.saveResource(fileName, false)
        }
    }

    fun getConfig(): FileConfiguration {
        ensureFileExists()
        return config
    }

    fun saveConfig() {
        try {
            config.save(file)
        } catch (e: IOException) {
            plugin.logger.severe("Could not save config $fileName: ${e.message}")
        }
    }

    fun reloadConfig() {
        ensureFileExists()
        config = YamlConfiguration.loadConfiguration(file)
    }

    companion object {
        private val instances = mutableListOf<ConfigManager>()

        fun reloadAll() {
            instances.forEach { it.reloadConfig() }
        }
    }
}
