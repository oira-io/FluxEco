package io.oira.fluxeco.core.command

import io.oira.fluxeco.core.FluxEco
import io.oira.fluxeco.core.command.permissions.ConfigPermission
import io.oira.fluxeco.core.lamp.AsyncOfflinePlayer
import io.oira.fluxeco.core.manager.ConfigManager
import io.oira.fluxeco.core.manager.EconomyManager
import io.oira.fluxeco.core.manager.MessageManager
import io.oira.fluxeco.core.util.Placeholders
import io.oira.fluxeco.core.util.Threads
import io.oira.fluxeco.core.util.format
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

    /**
     * Displays the command sender's current economy balance using the configured message templates.
     */
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

    /**
     * Shows another player's balance to the command sender.
     *
     * @param sender The player who invoked the command and will receive the message.
     * @param target The target player whose balance will be looked up and displayed (may be online or offline).
     */
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