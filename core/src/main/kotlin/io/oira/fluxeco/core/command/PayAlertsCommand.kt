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

package io.oira.fluxeco.core.command

import io.oira.fluxeco.core.FluxEco
import io.oira.fluxeco.core.command.permissions.ConfigPermission
import io.oira.fluxeco.core.manager.ConfigManager
import io.oira.fluxeco.core.manager.MessageManager
import io.oira.fluxeco.core.manager.SettingsManager
import io.oira.fluxeco.core.manager.SoundManager
import org.bukkit.entity.Player
import revxrsal.commands.annotation.*
import revxrsal.commands.orphan.OrphanCommand

class PayAlertsCommand : OrphanCommand {

    private val plugin: FluxEco = FluxEco.instance
    private val messageManager: MessageManager = MessageManager.getInstance()
    private val configManager = ConfigManager(plugin, "messages.yml")
    private val foliaLib = FluxEco.instance.foliaLib

    @CommandPlaceholder
    @Description("Toggles whether you receive payment alert messages.")
    @ConfigPermission("commands.pay-alerts.permission")
    fun payAlerts(sender: Player, @Optional @Named("toggle") @Suggest("on", "off") toggle: String?) {
        val newState = when (toggle?.lowercase()) {
            "on" -> {
                SettingsManager.setPayAlerts(sender.uniqueId, true)
                true
            }
            "off" -> {
                SettingsManager.setPayAlerts(sender.uniqueId, false)
                false
            }
            null -> SettingsManager.togglePayAlerts(sender.uniqueId)
            else -> {
                messageManager.sendMessageFromConfig(sender, "general.invalid-amount", config = configManager) // or a better message
                SoundManager.getInstance().playErrorSound(sender, configManager)
                return
            }
        }
        foliaLib.scheduler.run {
            if (newState) {
                messageManager.sendMessageFromConfig(sender, "payalerts.enabled", config = configManager)
            } else {
                messageManager.sendMessageFromConfig(sender, "payalerts.disabled", config = configManager)
            }
            SoundManager.getInstance().playTeleportSound(sender, configManager)
        }
    }
}
