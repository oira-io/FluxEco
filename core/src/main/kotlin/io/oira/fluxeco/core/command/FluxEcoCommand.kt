package io.oira.fluxeco.core.command

import io.oira.fluxeco.core.FluxEco
import io.oira.fluxeco.core.manager.ConfigManager
import io.oira.fluxeco.core.manager.MessageManager
import io.oira.fluxeco.core.util.NumberFormatter
import io.oira.fluxeco.core.util.Placeholders
import org.bukkit.entity.Player
import revxrsal.commands.annotation.CommandPlaceholder
import revxrsal.commands.annotation.Description
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.bukkit.annotation.CommandPermission
import revxrsal.commands.orphan.OrphanCommand

class FluxEcoCommand : OrphanCommand {

    private val plugin: FluxEco = FluxEco.instance
    private val messageManager: MessageManager = MessageManager.getInstance()
    private val configManager = ConfigManager(plugin, "messages.yml")

    @CommandPlaceholder
    fun onCommand() {}

    @Subcommand("reload")
    @Description("Reloads the plugin configuration.")
    @CommandPermission("fluxeco.command.fluxeco.reload")
    fun reload(sender: Player) {
        val start = System.currentTimeMillis()
        plugin.reloadConfig()
        ConfigManager.reloadAll()
        messageManager.reload()
        plugin.baltopGui.reload()
        plugin.historyGui.reload()
        NumberFormatter.reload()
        val end = System.currentTimeMillis()
        val ms = end - start
        val placeholders = Placeholders().add("ms", ms.toString())
        messageManager.sendMessageFromConfig(sender, "fluxeco.reload-success", placeholders, config = configManager)
    }

    @Subcommand("version")
    @Description("Shows the plugin version.")
    @CommandPermission("fluxeco.command.fluxeco.version")
    fun version(sender: Player) {
        val version = plugin.description.version
        val placeholders = Placeholders().add("version", version)
        messageManager.sendMessageFromConfig(sender, "fluxeco.version", placeholders, config = configManager)
    }
}
