package io.oira.fluxeco.core.command

import io.oira.fluxeco.FluxEco
import io.oira.fluxeco.core.lamp.AsyncOfflinePlayer
import io.oira.fluxeco.core.util.Threads
import org.bukkit.entity.Player
import revxrsal.commands.annotation.CommandPlaceholder
import revxrsal.commands.annotation.Description
import revxrsal.commands.annotation.Named
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.bukkit.annotation.CommandPermission
import revxrsal.commands.orphan.OrphanCommand

class StatsCommand : OrphanCommand {

    private val plugin: FluxEco = FluxEco.instance
    private val foliaLib = FluxEco.instance.foliaLib

    @CommandPlaceholder
    @Description("Opens the stats GUI.")
    @CommandPermission("fluxeco.command.stats")
    fun stats(sender: Player) {
        FluxEco.instance.statsGui.open(sender)
    }

    @Subcommand("other")
    @Description("Opens the stats GUI for a specific player.")
    @CommandPermission("fluxeco.command.stats.others")
    fun statsOther(sender: Player, @Named("target") target: AsyncOfflinePlayer) {
        Threads.runAsync {
            val offlinePlayer = target.getOrFetch()
            foliaLib.scheduler.run {
                FluxEco.instance.statsGui.openForPlayer(sender, offlinePlayer.uniqueId)
            }
        }
    }
}
