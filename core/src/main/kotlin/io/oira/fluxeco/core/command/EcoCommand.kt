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

    /**
     * Delivers a configured economy message to a player; if the player is offline and Redis is enabled, publishes the notification over Redis.
     *
     * If the target player is online, sends the message using the configured message manager. If the player is offline and Redis is enabled, publishes an economy notification with the provided placeholders.
     *
     * @param targetPlayer The target player (may be offline).
     * @param messageKey The message key from configuration to send or publish.
     * @param placeholders Placeholders to substitute into the message.
     */
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

    /**
     * Defines the root placeholder command for economy-related subcommands.
     *
     * Present solely to attach permission and placeholder metadata; performs no runtime action.
     */
    @CommandPlaceholder
    @ConfigPermission("commands.economy.permissions.base")
    fun onCommand() {}

    /**
     * Gives the specified amount of money to the target player, records the admin transaction, and notifies both sender and recipient.
     *
     * If the amount string cannot be parsed or is less than or equal to zero, sends an "invalid amount" message to the actor and plays an error sound.
     *
     * On success, the target's balance is increased, an admin receive transaction is recorded (using the actor's UUID or the console UUID when the actor is not a player), the actor receives a success message with placeholders for player, amount, and new balance, the target receives a receive-money message with the same placeholders, and a success sound is played for the actor.
     *
     * @param actor The command sender (player or console) performing the operation.
     * @param target The asynchronous offline-player reference for the recipient; the player will be fetched if necessary.
     * @param amount The amount to give as a string; must parse to a positive number.
     */
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

    /**
     * Give the specified amount to every online player.
     *
     * Parses the provided amount and, if valid and greater than zero, asynchronously adds that amount to each online player's balance, records an admin receipt transaction for each (using the console UUID when the actor is not a player), sends a receive message to each player, then sends a single success message to the actor and plays a teleport sound. If the amount is invalid or not greater than zero, sends an invalid-amount message to the actor and plays an error sound.
     *
     * @param amount The amount to give, provided as a string that must parse to a number greater than zero.
     */
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

    /**
     * Removes the specified amount of currency from a target player's balance.
     *
     * Parses the provided amount string; if parsing fails or the parsed value is less than or equal to zero,
     * the actor is notified and the command aborts. The balance subtraction and transaction recording occur
     * asynchronously. On success, an admin-deduction transaction is recorded (using the actor's UUID or the
     * console UUID when the actor is not a player), the actor and target are notified with updated balance
     * placeholders, and a success sound is played. On failure, the actor is notified of insufficient funds and
     * an error sound is played.
     *
     * @param actor The command invoker.
     * @param target An async resolver for the target offline player whose balance will be adjusted.
     * @param amount A numeric string representing the amount to remove from the target's balance.
     */
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

    /**
     * Subtracts the specified amount from every online player's balance, records admin deductions, and notifies players and the command issuer.
     *
     * @param amount The amount string to parse as a numeric value to subtract from each online player; if parsing fails or the parsed value is less than or equal to zero, the command aborts and an "invalid-amount" message is sent to the actor.
     *
     * Behaviour:
     * - Uses the actor's player UUID as the admin identifier, or the Console UUID if the actor is not a player.
     * - For each online player: attempts to subtract the parsed amount; on success records an admin deduction, sends the player an "economy.money-taken" message with placeholders (`player`, `amount`, `balance`); on failure sends the actor an "economy.insufficient-funds" message for that player.
     * - After processing all players, sends the actor an "economy.take-all-success" message (placeholder `amount`) and plays the teleport sound.
     */
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

    /**
     * Sets the specified player's balance to the provided amount.
     *
     * Records an admin transaction for the change (records a receive if the balance increases,
     * records a deduct if the balance decreases), sends confirmation messages to the command actor
     * and the target player, and plays the configured notification sound.
     *
     * @param actor The command actor performing the action.
     * @param target The offline player whose balance will be set.
     * @param amount The amount to set, provided as a string and parsed as a number; must be greater than or equal to 0.
     */
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

    /**
     * Sets every online player's balance to the given amount, records corresponding admin transactions, notifies each affected player and the actor, and plays a confirmation sound.
     *
     * If the amount cannot be parsed or is negative, sends "general.invalid-amount" to the actor and plays an error sound without modifying balances.
     *
     * @param actor The command sender performing the change.
     * @param amount The amount to set for each online player (as a parseable numeric string).
     */
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

    /**
     * Reset a player's balance to zero and notify the issuer and the target.
     *
     * Records an admin deduction if the player's previous balance was greater than zero, sends configured
     * confirmation messages to the command actor and the target player, and plays the configured notification sound.
     *
     * @param actor The command issuer (player or console).
     * @param target An asynchronous reference to the offline player whose balance will be reset.
     */
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

    /**
     * Reset balances of all online players to zero and notify affected players and the actor.
     *
     * Runs asynchronously: for each online player the balance is set to 0.0 and, if the previous
     * balance was greater than 0, an admin deduction transaction is recorded using the actor's
     * UUID (or the console UUID when the actor is not a player). Each affected player receives
     * the "economy.balance-reset" message and, after all players are processed, the actor
     * receives the "economy.reset-all-success" message and a teleport sound is played.
     */
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