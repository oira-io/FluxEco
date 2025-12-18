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

package io.oira.fluxeco.core.util

import io.oira.fluxeco.core.manager.ConfigManager
import org.bukkit.plugin.java.JavaPlugin
import java.text.SimpleDateFormat
import java.util.*

object DateFormatter {

    private lateinit var configManager: ConfigManager

    private var dateFormatPattern = "yyyy-MM-dd HH:mm:ss"
    private lateinit var dateFormat: SimpleDateFormat

    fun init(plugin: JavaPlugin, manager: ConfigManager) {
        configManager = manager
        reload()
    }

    fun reload() {
        val config = configManager.getConfig()
        dateFormatPattern = config.getString("format.date-format", "yyyy-MM-dd HH:mm:ss") ?: "yyyy-MM-dd HH:mm:ss"
        dateFormat = SimpleDateFormat(dateFormatPattern)
    }

    fun format(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }
}
