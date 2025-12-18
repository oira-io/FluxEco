package io.oira.fluxeco.core.command

import io.oira.fluxeco.FluxEco
import io.oira.fluxeco.core.command.permissions.ConfigPermission
import io.oira.fluxeco.core.manager.EconomyManager
import io.oira.fluxeco.core.manager.ConfigManager
import io.oira.fluxeco.core.manager.MessageManager
import io.oira.fluxeco.core.util.Placeholders
import io.oira.fluxeco.core.util.format
import io.oira.fluxeco.core.lamp.AsyncOfflinePlayer
import org.bukkit.entity.Player
import revxrsal.commands.annotation.Description
import revxrsal.commands.annotation.Named
import io.oira.fluxeco.core.util.Threads
import revxrsal.commands.annotation.CommandPlaceholder
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
