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

import io.github.miniplaceholders.kotlin.asInsertingTag
import io.github.miniplaceholders.kotlin.audience
import io.github.miniplaceholders.kotlin.expansion
import io.github.miniplaceholders.kotlin.global
import io.oira.fluxeco.core.cache.CacheManager
import io.oira.fluxeco.core.manager.SettingsManager
import io.oira.fluxeco.core.util.format
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class MiniPlaceholders {

    fun register() {
        val expansion = expansion("fluxeco") {
            audience<Player>("balance") { player, _, _ ->
                Component.text(CacheManager.getBalance(player.uniqueId).toString()).asInsertingTag()
            }
            audience<Player>("balance_formatted") { player, _, _ ->
                Component.text(CacheManager.getBalance(player.uniqueId).format()).asInsertingTag()
            }
            audience<Player>("toggle_payments") { player, _, _ ->
                Component.text(if (SettingsManager.getTogglePayments(player.uniqueId)) "enabled" else "disabled").asInsertingTag()
            }
            audience<Player>("pay_alerts") { player, _, _ ->
                Component.text(if (SettingsManager.getPayAlerts(player.uniqueId)) "enabled" else "disabled").asInsertingTag()
            }
            global("tps") { _, _ ->
                Component.text(Bukkit.getTPS()[0]).asInsertingTag()
            }
        }

        expansion.register()
    }

}