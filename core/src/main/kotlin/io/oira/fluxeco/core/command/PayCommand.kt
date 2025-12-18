package io.oira.fluxeco.core.command

import io.oira.fluxeco.core.FluxEco
import io.oira.fluxeco.core.command.permissions.ConfigPermission
import io.oira.fluxeco.core.lamp.AsyncOfflinePlayer
import io.oira.fluxeco.core.manager.*
import io.oira.fluxeco.core.redis.RedisManager
import io.oira.fluxeco.core.util.Placeholders
import io.oira.fluxeco.core.util.Threads
import io.oira.fluxeco.core.util.format
import io.oira.fluxeco.core.util.parseNum
import org.bukkit.entity.Player
import revxrsal.commands.annotation.CommandPlaceholder
import revxrsal.commands.annotation.Description
import revxrsal.commands.annotation.Named
import revxrsal.commands.orphan.OrphanCommand

class PayCommand : OrphanCommand {

    private val plugin: FluxEco = FluxEco.instance
    private val messageManager: MessageManager = MessageManager.getInstance()
    private val messagesConfigManager = ConfigManager(plugin, "messages.yml")
    private val mainConfigManager = ConfigManager(plugin, "config.yml")
    private val foliaLib = FluxEco.instance.foliaLib

    /**
     * Initiates a player-to-player payment for the specified amount.
     *
     * Parses and validates the provided amount, verifies recipient eligibility and sender balance,
     * and then either opens a confirmation UI (if enabled) or completes the transfer.
     * On success, updates both players' balances, records the transaction, and notifies sender and recipient
     * (online recipient is messaged directly; offline recipient may receive a Redis notification if enabled).
     * The command also emits appropriate feedback messages and sounds for all validation and outcome states.
     *
     * @param sender The player issuing the payment command.
     * @param target The recipient as an AsyncOfflinePlayer (may be online or offline).
     * @param amount The amount to transfer, provided as a string that will be parsed as a number.
     */
    @CommandPlaceholder
    @Description("Pays money to another player.")
    @ConfigPermission("commands.pay.permission")
    fun pay(sender: Player, @Named("target") target: AsyncOfflinePlayer, @Named("amount") amount: String) {
        val parsedAmount = try {
            amount.parseNum()
        } catch (_: Exception) {
            messageManager.sendMessageFromConfig(sender, "general.invalid-amount", config = messagesConfigManager)
            SoundManager.getInstance().playErrorSound(sender, messagesConfigManager)
            return
        }

        if (parsedAmount <= 0) {
            messageManager.sendMessageFromConfig(sender, "general.invalid-amount", config = messagesConfigManager)
            SoundManager.getInstance().playErrorSound(sender, messagesConfigManager)
            return
        }

        Threads.runAsync {
            val offlinePlayer = target.getOrFetch()
            if (offlinePlayer.player == null && !plugin.config.getBoolean("general.allow-offline-payments")) {
                foliaLib.scheduler.run {
                    messageManager.sendMessageFromConfig(sender, "pay.offline-disabled", config = messagesConfigManager)
                    SoundManager.getInstance().playErrorSound(sender, messagesConfigManager)
                }
                return@runAsync
            }
            if (offlinePlayer.uniqueId == sender.uniqueId) {
                foliaLib.scheduler.run {
                    messageManager.sendMessageFromConfig(sender, "pay.self", config = messagesConfigManager)
                    SoundManager.getInstance().playErrorSound(sender, messagesConfigManager)
                }
                return@runAsync
            }

            if (!SettingsManager.getTogglePayments(offlinePlayer.uniqueId)) {
                foliaLib.scheduler.run {
                    val placeholders = Placeholders().add("player", target.getName())
                    messageManager.sendMessageFromConfig(sender, "pay.disabled", placeholders, config = messagesConfigManager)
                    SoundManager.getInstance().playErrorSound(sender, messagesConfigManager)
                }
                return@runAsync
            }

            val senderBalance = EconomyManager.getBalance(sender.uniqueId)
            if (senderBalance < parsedAmount) {
                foliaLib.scheduler.run {
                    messageManager.sendMessageFromConfig(sender, "pay.insufficient-funds", config = messagesConfigManager)
                    SoundManager.getInstance().playErrorSound(sender, messagesConfigManager)
                }
                return@runAsync
            }

            val confirmPayments = mainConfigManager.getConfig().getBoolean("general.confirm-payments", false)
            if (confirmPayments) {
                foliaLib.scheduler.run {
                    plugin.confirmPaymentGui.openForPayment(sender, offlinePlayer, parsedAmount)
                }
                return@runAsync
            }

            val success = EconomyManager.subtractBalance(sender.uniqueId, parsedAmount)
            if (success) {
                EconomyManager.addBalance(offlinePlayer.uniqueId, parsedAmount)
                TransactionManager.recordTransfer(sender.uniqueId, offlinePlayer.uniqueId, parsedAmount)
                val newSenderBalance = EconomyManager.getBalance(sender.uniqueId)
                foliaLib.scheduler.run {
                    val placeholders = Placeholders()
                        .add("player", target.getName())
                        .add("amount", parsedAmount.format())
                        .add("balance", newSenderBalance.format())

                    messageManager.sendMessageFromConfig(sender, "pay.success", placeholders, config = messagesConfigManager)

                    if (SettingsManager.getPayAlerts(offlinePlayer.uniqueId)) {
                        val onlineTarget = offlinePlayer.player
                        if (onlineTarget != null && onlineTarget.isOnline) {
                            val targetPlaceholders = Placeholders()
                                .add("player", sender.name)
                                .add("amount", parsedAmount.format())
                            messageManager.sendMessageFromConfig(onlineTarget, "pay.receive", targetPlaceholders, config = messagesConfigManager)
                        } else if (RedisManager.isEnabled) {
                            RedisManager.getPublisher()?.publishPaymentNotification(
                                offlinePlayer.uniqueId,
                                sender.name,
                                parsedAmount,
                                parsedAmount.format()
                            )
                        }
                    }

                    SoundManager.getInstance().playTeleportSound(sender, messagesConfigManager)
                }
            } else {
                foliaLib.scheduler.run {
                    messageManager.sendMessageFromConfig(sender, "pay.insufficient-funds", config = messagesConfigManager)
                    SoundManager.getInstance().playErrorSound(sender, messagesConfigManager)
                }
            }
        }
    }
}