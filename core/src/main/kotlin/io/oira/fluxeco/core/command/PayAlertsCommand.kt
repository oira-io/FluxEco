package io.oira.fluxeco.core.command

import io.oira.fluxeco.core.FluxEco
import io.oira.fluxeco.core.command.permissions.ConfigPermission
import io.oira.fluxeco.core.manager.ConfigManager
import io.oira.fluxeco.core.manager.MessageManager
import io.oira.fluxeco.core.manager.SettingsManager
import io.oira.fluxeco.core.manager.SoundManager
import org.bukkit.entity.Player
import revxrsal.commands.annotation.*
import revxrsal.commands.orphan.OrphanCommand

class PayAlertsCommand : OrphanCommand {

    private val plugin: FluxEco = FluxEco.instance
    private val messageManager: MessageManager = MessageManager.getInstance()
    private val configManager = ConfigManager(plugin, "messages.yml")
    private val foliaLib = FluxEco.instance.foliaLib

    @CommandPlaceholder
    @Description("Toggles whether you receive payment alert messages.")
    @ConfigPermission("commands.pay-alerts.permission")
    fun payAlerts(sender: Player, @Optional @Named("toggle") @Suggest("on", "off") toggle: String?) {
        val newState = when (toggle?.lowercase()) {
            "on" -> {
                SettingsManager.setPayAlerts(sender.uniqueId, true)
                true
            }
            "off" -> {
                SettingsManager.setPayAlerts(sender.uniqueId, false)
                false
            }
            null -> SettingsManager.togglePayAlerts(sender.uniqueId)
            else -> {
                messageManager.sendMessageFromConfig(sender, "general.invalid-amount", config = configManager) // or a better message
                SoundManager.getInstance().playErrorSound(sender, configManager)
                return
            }
        }
        foliaLib.scheduler.run {
            if (newState) {
                messageManager.sendMessageFromConfig(sender, "payalerts.enabled", config = configManager)
            } else {
                messageManager.sendMessageFromConfig(sender, "payalerts.disabled", config = configManager)
            }
            SoundManager.getInstance().playTeleportSound(sender, configManager)
        }
    }
}
