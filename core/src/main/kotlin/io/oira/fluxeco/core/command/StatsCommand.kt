package io.oira.fluxeco.core.command

import io.oira.fluxeco.FluxEco
import io.oira.fluxeco.core.command.permissions.ConfigPermission
import io.oira.fluxeco.core.lamp.AsyncOfflinePlayer
import io.oira.fluxeco.core.util.Threads
import org.bukkit.entity.Player
import revxrsal.commands.annotation.CommandPlaceholder
import revxrsal.commands.annotation.Description
import revxrsal.commands.annotation.Named
import revxrsal.commands.orphan.OrphanCommand

class StatsCommand : OrphanCommand {

    private val foliaLib = FluxEco.instance.foliaLib

    @CommandPlaceholder
    @Description("Opens the stats GUI.")
    @ConfigPermission("commands.stats.permissions.base")
    fun stats(sender: Player) {
        FluxEco.instance.statsGui.open(sender)
    }

    @Description("Opens the stats GUI for a specific player.")
    @ConfigPermission("commands.stats.permissions.others")
    fun statsOther(sender: Player, @Named("target") target: AsyncOfflinePlayer) {
        Threads.runAsync {
            val offlinePlayer = target.getOrFetch()
            foliaLib.scheduler.run {
                FluxEco.instance.statsGui.openForPlayer(sender, offlinePlayer.uniqueId)
            }
        }
    }
}
