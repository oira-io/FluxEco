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
import io.oira.fluxeco.manager.ConfigManager
import io.oira.fluxeco.manager.EconomyManager
import io.oira.fluxeco.manager.MessageManager
import io.oira.fluxeco.util.Placeholders
import io.oira.fluxeco.util.Threads
import io.oira.fluxeco.util.format
import org.bukkit.entity.Player
import revxrsal.commands.annotation.CommandPlaceholder
import revxrsal.commands.annotation.Description
import revxrsal.commands.annotation.Named
import revxrsal.commands.orphan.OrphanCommand

class BalanceCommand : OrphanCommand {

    private val plugin: FluxEco = FluxEco.instance
    private val messageManager: MessageManager = MessageManager.getInstance()
    private val configManager = ConfigManager(plugin, "messages.yml")
    private val foliaLib = FluxEco.instance.foliaLib

    @CommandPlaceholder
    @Description("Shows your current balance.")
    @ConfigPermission("commands.balance.permissions.base")
    fun balance(sender: Player) {
        val player = AsyncOfflinePlayer.from(sender)

        Threads.runAsync {
            val offlinePlayer = player.getOrFetch()
            val balance = EconomyManager.getBalance(offlinePlayer.uniqueId)
            foliaLib.scheduler.run {
                val placeholders = Placeholders()
                    .add("player", player.getName())
                    .add("balance", balance.format())

                messageManager.sendMessageFromConfig(sender, "balance.self", placeholders, config = configManager)
            }
        }
    }

    @Description("Shows another player's balance.")
    @ConfigPermission("commands.balance.permissions.others")
    fun balanceOther(sender: Player, @Named("target") target: AsyncOfflinePlayer) {
        Threads.runAsync {
            val offlinePlayer = target.getOrFetch()
            val balance = EconomyManager.getBalance(offlinePlayer.uniqueId)
            foliaLib.scheduler.run {
                val placeholders = Placeholders()
                    .add("player", target.getName())
                    .add("balance", balance.format())

                messageManager.sendMessageFromConfig(sender, "balance.other", placeholders, config = configManager)
            }
        }
    }
}
