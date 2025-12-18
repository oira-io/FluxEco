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

class StatsCommand : OrphanCommand {

    private val foliaLib = FluxEco.instance.foliaLib

    /**
     * Opens the global statistics GUI for the invoking player.
     *
     * @param sender The player who invoked the command; the GUI is opened for this player.
     */
    @CommandPlaceholder
    @Description("Opens the stats GUI.")
    @ConfigPermission("commands.stats.permissions.base")
    fun stats(sender: Player) {
        FluxEco.instance.statsGui.open(sender)
    }

    /**
     * Opens the stats GUI for a specific player's data and displays it to the invoking player.
     *
     * @param sender The player who will see the stats GUI.
     * @param target The offline player whose stats will be displayed.
     */
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