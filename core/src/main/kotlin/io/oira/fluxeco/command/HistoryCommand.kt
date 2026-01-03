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

package io.oira.fluxeco.command

import io.oira.fluxeco.FluxEco
import io.oira.fluxeco.lamp.annotation.ConfigPermission
import io.oira.fluxeco.lamp.AsyncOfflinePlayer
import io.oira.fluxeco.util.Threads
import org.bukkit.entity.Player
import revxrsal.commands.annotation.CommandPlaceholder
import revxrsal.commands.annotation.Description
import revxrsal.commands.annotation.Named
import revxrsal.commands.orphan.OrphanCommand

class HistoryCommand : OrphanCommand {

    private val foliaLib = FluxEco.instance.foliaLib

    @CommandPlaceholder
    @Description("Opens the transaction history GUI.")
    @ConfigPermission("commands.transaction-history.permissions.base")
    fun history(sender: Player) {
        FluxEco.instance.historyGui.open(sender)
    }

    @CommandPlaceholder
    @Description("Opens the transaction history GUI for a specific player.")
    @ConfigPermission("commands.transaction-history.permissions.others")
    fun historyOther(sender: Player, @Named("target") target: AsyncOfflinePlayer) {
        Threads.runAsync {
            val offlinePlayer = target.getOrFetch()
            foliaLib.scheduler.run {
                FluxEco.instance.historyGui.openForPlayer(sender, offlinePlayer.uniqueId)
            }
        }
    }
}
