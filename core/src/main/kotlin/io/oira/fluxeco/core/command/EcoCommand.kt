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
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import revxrsal.commands.annotation.CommandPlaceholder
import revxrsal.commands.annotation.Description
import revxrsal.commands.annotation.Named
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import revxrsal.commands.orphan.OrphanCommand
import java.util.*

class EcoCommand : OrphanCommand {

    private val plugin: FluxEco = FluxEco.instance
    private val messageManager: MessageManager = MessageManager.getInstance()
    private val configManager = ConfigManager(plugin, "messages.yml")
    private val foliaLib = FluxEco.instance.foliaLib

    private fun sendMessage(
        targetPlayer: OfflinePlayer,
        messageKey: String,
        placeholders: Placeholders
    ) {
        val onlinePlayer = targetPlayer.player
        if (onlinePlayer != null && onlinePlayer.isOnline) {
            messageManager.sendMessageFromConfig(onlinePlayer, messageKey, placeholders, config = configManager)
        } else if (RedisManager.isEnabled) {
            RedisManager.getPublisher()?.publishEconomyNotification(
                targetPlayer.uniqueId,
                messageKey,
                placeholders.toMap()
            )
        }
    }

    @CommandPlaceholder
    @ConfigPermission("commands.economy.permissions.base")
    fun onCommand() {}

    @Subcommand("give")
    @Description("Gives money to a player.")
    @ConfigPermission("commands.economy.permissions.give")
    fun giveCommand(actor: BukkitCommandActor, @Named("target") target: AsyncOfflinePlayer, @Named("amount") amount: String) {
        val parsedAmount = try {
            amount.parseNum()
        } catch (_: Exception) {
            messageManager.sendMessageFromConfig(actor, "general.invalid-amount", config = configManager)
            SoundManager.getInstance().playErrorSound(actor, configManager)
            return
        }

        if (parsedAmount <= 0) {
            messageManager.sendMessageFromConfig(actor, "general.invalid-amount", config = configManager)
            SoundManager.getInstance().playErrorSound(actor, configManager)
            return
        }

        target.getOrFetchAsync().thenAccept { offlinePlayer ->
            EconomyManager.addBalanceAsync(offlinePlayer.uniqueId, parsedAmount).thenAccept { _ ->
                val adminUuid = actor.asPlayer()?.uniqueId ?: UUID(0, 0)
                TransactionManager.recordAdminReceive(offlinePlayer.uniqueId, parsedAmount, adminUuid)
                EconomyManager.getBalanceAsync(offlinePlayer.uniqueId).thenAccept { newBalance ->
                    foliaLib.scheduler.run {
                        val placeholders = Placeholders()
                            .add("player", actor.asPlayer()?.name ?: "Console")
                            .add("amount", parsedAmount.format())
                            .add("balance", newBalance.format())

                        messageManager.sendMessageFromConfig(actor, "economy.give-success", placeholders, config = configManager)

                        sendMessage(offlinePlayer, "economy.receive-money", placeholders)

                        SoundManager.getInstance().playTeleportSound(actor, configManager)
                    }
                }
            }
        }
    }

    @Subcommand("give *")
    @Description("Gives money to all online players.")
    @ConfigPermission("commands.economy.permissions.give-all")
    fun giveCommandAll(actor: BukkitCommandActor, @Named("amount") amount: String) {
        val parsedAmount = try {
            amount.parseNum()
        } catch (_: Exception) {
            messageManager.sendMessageFromConfig(actor, "general.invalid-amount", config = configManager)
            SoundManager.getInstance().playErrorSound(actor, configManager)
            return
        }

        if (parsedAmount <= 0) {
            messageManager.sendMessageFromConfig(actor, "general.invalid-amount", config = configManager)
            SoundManager.getInstance().playErrorSound(actor, configManager)
            return
        }

        val adminUuid = actor.asPlayer()?.uniqueId ?: UUID(0, 0)
        Threads.runAsync {
            for (player in Bukkit.getOnlinePlayers()) {
                EconomyManager.addBalance(player.uniqueId, parsedAmount)
                TransactionManager.recordAdminReceive(player.uniqueId, parsedAmount, adminUuid)
                val newBalance = EconomyManager.getBalance(player.uniqueId)
                foliaLib.scheduler.run {
                    val placeholders = Placeholders()
                        .add("player", actor.asPlayer()?.name ?: "Console")
                        .add("amount", parsedAmount.format())
                        .add("balance", newBalance.format())
                    messageManager.sendMessageFromConfig(player, "economy.receive-money", placeholders, config = configManager)
                }
            }
            foliaLib.scheduler.run {
                messageManager.sendMessageFromConfig(actor.sender(), "economy.give-all-success", Placeholders().add("amount", parsedAmount.format()), config = configManager)
                SoundManager.getInstance().playTeleportSound(actor, configManager)
            }
        }
    }

    @Subcommand("take")
    @Description("Takes money from a player.")
    @ConfigPermission("commands.economy.permissions.take")
    fun takeCommand(actor: BukkitCommandActor, @Named("target") target: AsyncOfflinePlayer, @Named("amount") amount: String) {
        val parsedAmount = try {
            amount.parseNum()
        } catch (_: Exception) {
            messageManager.sendMessageFromConfig(actor, "general.invalid-amount", config = configManager)
            SoundManager.getInstance().playErrorSound(actor, configManager)
            return
        }

        if (parsedAmount <= 0) {
            messageManager.sendMessageFromConfig(actor, "general.invalid-amount", config = configManager)
            SoundManager.getInstance().playErrorSound(actor, configManager)
            return
        }

        val adminUuid = actor.asPlayer()?.uniqueId ?: UUID(0, 0)
        Threads.runAsync {
            val offlinePlayer = target.getOrFetch()
            val success = EconomyManager.subtractBalance(offlinePlayer.uniqueId, parsedAmount)
            foliaLib.scheduler.run {
                if (success) {
                    TransactionManager.recordAdminDeduct(offlinePlayer.uniqueId, parsedAmount, adminUuid)
                    val newBalance = EconomyManager.getBalance(offlinePlayer.uniqueId)
                    val placeholders = Placeholders()
                        .add("player", actor.asPlayer()?.name ?: "Console")
                        .add("amount", parsedAmount.format())
                        .add("balance", newBalance.format())

                    messageManager.sendMessageFromConfig(actor, "economy.take-success", placeholders, config = configManager)

                    sendMessage(offlinePlayer, "economy.money-taken", placeholders)

                    SoundManager.getInstance().playTeleportSound(actor, configManager)
                } else {
                    messageManager.sendMessageFromConfig(actor, "economy.insufficient-funds", Placeholders().add("player", target.getName()), config = configManager)
                    SoundManager.getInstance().playErrorSound(actor, configManager)
                }
            }
        }
    }

    @Subcommand("take *")
    @Description("Takes money from all online players.")
    @ConfigPermission("commands.economy.permissions.take-all")
    fun takeCommandAll(actor: BukkitCommandActor, @Named("amount") amount: String) {
        val parsedAmount = try {
            amount.parseNum()
        } catch (_: Exception) {
            messageManager.sendMessageFromConfig(actor, "general.invalid-amount", config = configManager)
            SoundManager.getInstance().playErrorSound(actor, configManager)
            return
        }

        if (parsedAmount <= 0) {
            messageManager.sendMessageFromConfig(actor, "general.invalid-amount", config = configManager)
            SoundManager.getInstance().playErrorSound(actor, configManager)
            return
        }

        val adminUuid = actor.asPlayer()?.uniqueId ?: UUID(0, 0)
        Threads.runAsync {
            for (player in Bukkit.getOnlinePlayers()) {
                val success = EconomyManager.subtractBalance(player.uniqueId, parsedAmount)
                foliaLib.scheduler.run {
                    if (success) {
                        TransactionManager.recordAdminDeduct(player.uniqueId, parsedAmount, adminUuid)
                        val newBalance = EconomyManager.getBalance(player.uniqueId)
                        val placeholders = Placeholders()
                            .add("player", actor.asPlayer()?.name ?: "Console")
                            .add("amount", parsedAmount.format())
                            .add("balance", newBalance.format())
                        messageManager.sendMessageFromConfig(player, "economy.money-taken", placeholders, config = configManager)
                    } else {
                        messageManager.sendMessageFromConfig(actor.sender(), "economy.insufficient-funds", Placeholders().add("player", player.name), config = configManager)
                    }
                }
            }
            foliaLib.scheduler.run {
                messageManager.sendMessageFromConfig(actor.sender(), "economy.take-all-success", Placeholders().add("amount", parsedAmount.format()), config = configManager)
                SoundManager.getInstance().playTeleportSound(actor, configManager)
            }
        }
    }

    @Subcommand("set")
    @Description("Sets a player's balance.")
    @ConfigPermission("commands.economy.permissions.set")
    fun setCommand(actor: BukkitCommandActor, @Named("target") target: AsyncOfflinePlayer, @Named("amount") amount: String) {
        val parsedAmount = try {
            amount.parseNum()
        } catch (_: Exception) {
            messageManager.sendMessageFromConfig(actor, "general.invalid-amount", config = configManager)
            SoundManager.getInstance().playErrorSound(actor, configManager)
            return
        }

        if (parsedAmount < 0) {
            messageManager.sendMessageFromConfig(actor, "general.invalid-amount", config = configManager)
            SoundManager.getInstance().playErrorSound(actor, configManager)
            return
        }

        val adminUuid = actor.asPlayer()?.uniqueId ?: UUID(0, 0)
        Threads.runAsync {
            val offlinePlayer = target.getOrFetch()
            val oldBalance = EconomyManager.getBalance(offlinePlayer.uniqueId)
            EconomyManager.setBalance(offlinePlayer.uniqueId, parsedAmount)
            val diff = parsedAmount - oldBalance
            if (diff > 0) TransactionManager.recordAdminReceive(offlinePlayer.uniqueId, diff, adminUuid)
            else if (diff < 0) TransactionManager.recordAdminDeduct(offlinePlayer.uniqueId, -diff, adminUuid)

            foliaLib.scheduler.run {
                val placeholders = Placeholders()
                    .add("player", actor.asPlayer()?.name ?: "Console")
                    .add("amount", parsedAmount.format())
                    .add("balance", parsedAmount.format())

                messageManager.sendMessageFromConfig(actor, "economy.set-success", placeholders, config = configManager)

                sendMessage(offlinePlayer, "economy.balance-set", placeholders)

                SoundManager.getInstance().playTeleportSound(actor, configManager)
            }
        }
    }

    @Subcommand("set *")
    @Description("Sets all online players' balance.")
    @ConfigPermission("commands.economy.permissions.set-all")
    fun setCommandAll(actor: BukkitCommandActor, @Named("amount") amount: String) {
        val parsedAmount = try {
            amount.parseNum()
        } catch (_: Exception) {
            messageManager.sendMessageFromConfig(actor, "general.invalid-amount", config = configManager)
            SoundManager.getInstance().playErrorSound(actor, configManager)
            return
        }

        if (parsedAmount < 0) {
            messageManager.sendMessageFromConfig(actor, "general.invalid-amount", config = configManager)
            SoundManager.getInstance().playErrorSound(actor, configManager)
            return
        }

        val adminUuid = actor.asPlayer()?.uniqueId ?: UUID(0, 0)
        Threads.runAsync {
            for (player in Bukkit.getOnlinePlayers()) {
                val oldBalance = EconomyManager.getBalance(player.uniqueId)
                EconomyManager.setBalance(player.uniqueId, parsedAmount)
                val diff = parsedAmount - oldBalance
                if (diff > 0) TransactionManager.recordAdminReceive(player.uniqueId, diff, adminUuid)
                else if (diff < 0) TransactionManager.recordAdminDeduct(player.uniqueId, -diff, adminUuid)

                foliaLib.scheduler.run {
                    val placeholders = Placeholders()
                        .add("player", actor.asPlayer()?.name ?: "Console")
                        .add("amount", parsedAmount.format())
                        .add("balance", parsedAmount.format())

                    messageManager.sendMessageFromConfig(player, "economy.balance-set", placeholders, config = configManager)
                }
            }
            foliaLib.scheduler.run {
                messageManager.sendMessageFromConfig(actor.sender(), "economy.set-all-success", Placeholders().add("amount", parsedAmount.format()), config = configManager)
                SoundManager.getInstance().playTeleportSound(actor, configManager)
            }
        }
    }

    @Subcommand("reset")
    @Description("Resets a player's balance to 0.")
    @ConfigPermission("commands.economy.permissions.reset")
    fun resetCommand(actor: BukkitCommandActor, @Named("target") target: AsyncOfflinePlayer) {
        val adminUuid = actor.asPlayer()?.uniqueId ?: UUID(0, 0)
        Threads.runAsync {
            val offlinePlayer = target.getOrFetch()
            val oldBalance = EconomyManager.getBalance(offlinePlayer.uniqueId)
            EconomyManager.setBalance(offlinePlayer.uniqueId, 0.0)
            if (oldBalance > 0) TransactionManager.recordAdminDeduct(offlinePlayer.uniqueId, oldBalance, adminUuid)
            foliaLib.scheduler.run {
                val placeholders = Placeholders()
                    .add("player", actor.asPlayer()?.name ?: "Console")
                    .add("balance", "0.0")

                messageManager.sendMessageFromConfig(actor, "economy.reset-success", placeholders, config = configManager)

                sendMessage(offlinePlayer, "economy.balance-reset", placeholders)

                SoundManager.getInstance().playTeleportSound(actor, configManager)
            }
        }
    }

    @Subcommand("reset *")
    @Description("Resets all online players' balance to 0.")
    @ConfigPermission("commands.economy.permissions.reset-all")
    fun resetCommandAll(actor: BukkitCommandActor) {
        val adminUuid = actor.asPlayer()?.uniqueId ?: UUID(0, 0)
        Threads.runAsync {
            for (player in Bukkit.getOnlinePlayers()) {
                val oldBalance = EconomyManager.getBalance(player.uniqueId)
                EconomyManager.setBalance(player.uniqueId, 0.0)
                if (oldBalance > 0) TransactionManager.recordAdminDeduct(player.uniqueId, oldBalance, adminUuid)
                foliaLib.scheduler.run {
                    val placeholders = Placeholders()
                        .add("player", actor.asPlayer()?.name ?: "Console")
                        .add("balance", "0.0")
                    messageManager.sendMessageFromConfig(player, "economy.balance-reset", placeholders, config = configManager)
                }
            }
            messageManager.sendMessageFromConfig(actor.sender(), "economy.reset-all-success", null, config = configManager)
            SoundManager.getInstance().playTeleportSound(actor, configManager)
        }
    }
}
