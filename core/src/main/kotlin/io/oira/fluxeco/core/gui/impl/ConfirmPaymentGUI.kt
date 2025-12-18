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

package io.oira.fluxeco.core.gui.impl

import io.oira.fluxeco.core.gui.BaseGUI
import io.oira.fluxeco.core.manager.*
import io.oira.fluxeco.core.redis.RedisManager
import io.oira.fluxeco.core.util.Placeholders
import io.oira.fluxeco.core.util.Threads
import io.oira.fluxeco.core.util.format
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.SkullMeta

class ConfirmPaymentGUI : BaseGUI("gui/confirm-ui.yml") {

    private var targetPlayer: OfflinePlayer? = null
    private var amount: Double = 0.0
    private var sender: Player? = null

    init {
        registerActions()
        initialize()
    }

    private fun registerActions() {
        registerSimpleAction("confirm") { player -> handleConfirm(player) }
        registerSimpleAction("cancel") { player -> handleCancel(player) }
    }

    private fun handleConfirm(player: Player) {
        val target = targetPlayer ?: return
        val paymentAmount = amount

        if (paymentAmount <= 0) {
            player.closeInventory()
            return
        }

        player.closeInventory()

        Threads.runAsync {
            val senderBalance = EconomyManager.getBalance(player.uniqueId)
            if (senderBalance < paymentAmount) {
                plugin.foliaLib.scheduler.run {
                    val msgConfig = ConfigManager(plugin, "messages.yml")
                    messageManager.sendMessageFromConfig(player, "pay.insufficient-funds", config = msgConfig)
                    SoundManager.getInstance().playErrorSound(player, msgConfig)
                }
                return@runAsync
            }

            val success = EconomyManager.subtractBalance(player.uniqueId, paymentAmount)
            if (success) {
                EconomyManager.addBalance(target.uniqueId, paymentAmount)
                TransactionManager.recordTransfer(player.uniqueId, target.uniqueId, paymentAmount)
                val newSenderBalance = EconomyManager.getBalance(player.uniqueId)

                plugin.foliaLib.scheduler.run {
                    val msgConfig = ConfigManager(plugin, "messages.yml")
                    val placeholders = Placeholders()
                        .add("player", target.name ?: "Unknown")
                        .add("amount", paymentAmount.format())
                        .add("balance", newSenderBalance.format())

                    messageManager.sendMessageFromConfig(player, "pay.success", placeholders, config = msgConfig)

                    if (SettingsManager.getPayAlerts(target.uniqueId)) {
                        val onlineTarget = target.player
                        if (onlineTarget != null && onlineTarget.isOnline) {
                            val targetPlaceholders = Placeholders()
                                .add("player", player.name)
                                .add("amount", paymentAmount.format())
                            messageManager.sendMessageFromConfig(onlineTarget, "pay.receive", targetPlaceholders, config = msgConfig)
                        } else if (RedisManager.isEnabled) {
                            RedisManager.getPublisher()?.publishPaymentNotification(
                                target.uniqueId,
                                player.name,
                                paymentAmount,
                                paymentAmount.format()
                            )
                        }
                    }

                    SoundManager.getInstance().playTeleportSound(player, msgConfig)
                }
            } else {
                plugin.foliaLib.scheduler.run {
                    val msgConfig = ConfigManager(plugin, "messages.yml")
                    messageManager.sendMessageFromConfig(player, "pay.insufficient-funds", config = msgConfig)
                    SoundManager.getInstance().playErrorSound(player, msgConfig)
                }
            }
        }
    }

    private fun handleCancel(player: Player) {
        player.closeInventory()
        soundManager.playSoundFromConfig(player, "close", configManager)
    }

    fun openForPayment(player: Player, target: OfflinePlayer, paymentAmount: Double) {
        this.sender = player
        this.targetPlayer = target
        this.amount = paymentAmount

        updatePlayerItem(target)
        super.open(player)
    }

    private fun updatePlayerItem(target: OfflinePlayer) {
        val playerItemConfig = config.getConfigurationSection("items.player") ?: return
        val slot = playerItemConfig.getInt("slot", 13)

        val placeholders = Placeholders().add("player", target.name ?: "Unknown")
        val item = createItemFromConfig(playerItemConfig, "player", placeholders) ?: return

        if (item.itemMeta is SkullMeta) {
            val meta = item.itemMeta as SkullMeta
            meta.owningPlayer = target
            item.itemMeta = meta
        }

        inventory.setItem(slot, item)
    }

    override fun reload() {
        super.reload()
        targetPlayer?.let { updatePlayerItem(it) }
    }
}

