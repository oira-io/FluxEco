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

package io.oira.fluxeco.core.gui.impl

import de.rapha149.signgui.SignGUI
import de.rapha149.signgui.SignGUIAction
import de.rapha149.signgui.exception.SignGUIVersionException
import io.oira.fluxeco.core.FluxEco
import io.oira.fluxeco.core.manager.ConfigManager
import io.oira.fluxeco.core.manager.MessageManager
import io.oira.fluxeco.core.manager.SoundManager
import org.bukkit.Material
import org.bukkit.entity.Player

class BaltopSignGUI(private val baltopGui: BaltopGUI) {

    private val plugin = FluxEco.instance
    private val configManager = ConfigManager(plugin, "gui/baltop-ui.yml")
    private val messageManager = MessageManager.getInstance()
    private val soundManager = SoundManager.getInstance()

    fun open(player: Player) {
        try {
            val gui = SignGUI.builder()
                .setLines(null, "↑↑↑↑↑↑↑↑↑↑↑↑", "Search Player", null)
                .setType(Material.OAK_SIGN)
                .setHandler { p, result ->
                    val line0 = result.getLine(0)
                    val query = line0?.trim() ?: ""

                    plugin.foliaLib.scheduler.runNextTick {
                        if (query.isEmpty()) {
                            baltopGui.clearSearch()
                        } else {
                            baltopGui.setSearchQuery(query)
                        }
                    }

                    emptyList<SignGUIAction>()
                }
                .build()
            gui.open(player)
        } catch (_: SignGUIVersionException) {
            messageManager.sendMessageFromConfig(player, "messages.sign-gui-not-supported", configManager)
            soundManager.playSoundFromConfig(player, "error", configManager)
        }
    }
}