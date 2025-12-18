package io.oira.fluxeco.core.command

import io.oira.fluxeco.core.FluxEco
import io.oira.fluxeco.core.command.permissions.ConfigPermission
import io.oira.fluxeco.core.lamp.AsyncOfflinePlayer
import io.oira.fluxeco.core.util.Threads
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
