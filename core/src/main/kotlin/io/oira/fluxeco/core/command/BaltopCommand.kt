package io.oira.fluxeco.core.command

import io.oira.fluxeco.FluxEco
import io.oira.fluxeco.core.command.permissions.ConfigPermission
import org.bukkit.entity.Player
import revxrsal.commands.annotation.CommandPlaceholder
import revxrsal.commands.annotation.Description
import revxrsal.commands.orphan.OrphanCommand

class BaltopCommand : OrphanCommand {

    @CommandPlaceholder
    @Description("Opens the balance leaderboard GUI.")
    @ConfigPermission("commands.balance-top.permission")
    fun baltop(sender: Player) {
        FluxEco.instance.baltopGui.open(sender)
    }
}
