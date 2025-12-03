package io.oira.fluxeco.core.command

import io.oira.fluxeco.FluxEco
import io.oira.fluxeco.core.lamp.AsyncOfflinePlayer
import io.oira.fluxeco.core.util.Threads
import org.bukkit.entity.Player
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Description
import revxrsal.commands.annotation.Named
import revxrsal.commands.bukkit.annotation.CommandPermission

@Command("history")
class HistoryCommand {

    private val plugin: FluxEco = FluxEco.instance
    private val foliaLib = FluxEco.instance.foliaLib

    @Description("Opens the transaction history GUI.")
    @CommandPermission("fluxeco.command.history")
    fun history(sender: Player) {
        FluxEco.instance.historyGui.open(sender)
    }

    @Description("Opens the transaction history GUI for a specific player.")
    @CommandPermission("fluxeco.command.history.others")
    fun historyOther(sender: Player, @Named("target") target: AsyncOfflinePlayer) {
        Threads.runAsync {
            val offlinePlayer = target.getOrFetch()
            foliaLib.scheduler.run {
                FluxEco.instance.historyGui.openForPlayer(sender, offlinePlayer.uniqueId)
            }
        }
    }
}
