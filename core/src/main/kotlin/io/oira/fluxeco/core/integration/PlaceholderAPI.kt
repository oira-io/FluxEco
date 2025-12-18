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

package io.oira.fluxeco.core.integration

import io.oira.fluxeco.core.cache.CacheManager
import io.oira.fluxeco.core.manager.SettingsManager
import io.oira.fluxeco.core.util.format
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.jetbrains.annotations.NotNull

class PlaceholderAPI : PlaceholderExpansion() {

    @NotNull
    override fun getAuthor(): String {
        return "Harfull"
    }

    @NotNull
    override fun getIdentifier(): String {
        return "FluxEco"
    }

    @NotNull
    override fun getVersion(): String {
        return "1.0.0"
    }

    override fun onRequest(player: OfflinePlayer?, @NotNull params: String): String? {
        if (player == null) return null
        return when (params.lowercase()) {
            "balance" -> CacheManager.getBalance(player.uniqueId).toString()
            "balance_formatted" -> CacheManager.getBalance(player.uniqueId).format()
            "toggle_payments" -> if (SettingsManager.getTogglePayments(player.uniqueId)) "enabled" else "disabled"
            "pay_alerts" -> if (SettingsManager.getPayAlerts(player.uniqueId)) "enabled" else "disabled"
            else -> null
        }
    }

    override fun onPlaceholderRequest(player: Player?, @NotNull params: String): String? {
        if (player == null) return null
        return when (params.lowercase()) {
            "balance" -> CacheManager.getBalance(player.uniqueId).toString()
            "balance_formatted" -> CacheManager.getBalance(player.uniqueId).format()
            "toggle_payments" -> if (SettingsManager.getTogglePayments(player.uniqueId)) "enabled" else "disabled"
            "pay_alerts" -> if (SettingsManager.getPayAlerts(player.uniqueId)) "enabled" else "disabled"
            else -> null
        }
    }
}
