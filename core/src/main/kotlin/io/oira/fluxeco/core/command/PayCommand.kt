/*
 * FluxEco
 * Copyright (C) 2025 Harfull
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

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