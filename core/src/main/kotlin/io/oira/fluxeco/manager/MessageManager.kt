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

package io.oira.fluxeco.manager

import io.oira.fluxeco.FluxEco
import io.oira.fluxeco.util.Placeholders
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import java.time.Duration
import java.util.regex.Pattern

@Suppress("unused")
class MessageManager private constructor() {

    private val miniMessage = MiniMessage.miniMessage()
    private val legacySerializer = LegacyComponentSerializer.legacyAmpersand()
    private val messagesConfig by lazy { ConfigManager(plugin, "messages.yml") }

    private val plugin: FluxEco = FluxEco.instance

    companion object {
        @Volatile
        private var instance: MessageManager? = null
        private val HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})")

        fun getInstance(): MessageManager = instance ?: synchronized(this) {
            instance ?: MessageManager().also { instance = it }
        }
    }

    fun sendMessage(player: Player, message: String) {
        sendMessage(player, message, null, true)
    }

    fun sendMessage(player: Player, message: String, placeholders: Placeholders?) {
        sendMessage(player, message, placeholders, true)
    }

    fun sendMessage(player: Player, message: String, prefix: Boolean) {
        sendMessage(player, message, null, prefix)
    }

    fun sendMessage(player: Player, message: String, placeholders: Placeholders?, prefix: Boolean) {
        val component = buildComponent(message, placeholders, prefix)
        player.sendMessage(component)
    }

    fun sendMessage(actor: BukkitCommandActor, message: String) {
        sendMessage(actor, message, null, true)
    }

    fun sendMessage(actor: BukkitCommandActor, message: String, placeholders: Placeholders?) {
        sendMessage(actor, message, placeholders, true)
    }

    fun sendMessage(actor: BukkitCommandActor, message: String, prefix: Boolean) {
        sendMessage(actor, message, null, prefix)
    }

    fun sendMessage(actor: BukkitCommandActor, message: String, placeholders: Placeholders?, prefix: Boolean) {
        val component = buildComponent(message, placeholders, prefix)
        actor.sender().sendMessage(component)
    }

    fun sendMessage(sender: ConsoleCommandSender, message: String) {
        sendMessage(sender, message, null, true)
    }

    fun sendMessage(sender: ConsoleCommandSender, message: String, placeholders: Placeholders?) {
        sendMessage(sender, message, placeholders, true)
    }

    fun sendMessage(sender: ConsoleCommandSender, message: String, prefix: Boolean) {
        sendMessage(sender, message, null, prefix)
    }

    fun sendMessage(sender: ConsoleCommandSender, message: String, placeholders: Placeholders?, prefix: Boolean) {
        val component = buildComponent(message,placeholders, prefix)
        sender.sendMessage(component)
    }

    fun sendMessage(sender: CommandSender, message: String) {
        sendMessage(sender, message, null, true)
    }

    fun sendMessage(sender: CommandSender, message: String, placeholders: Placeholders?) {
        sendMessage(sender, message, placeholders, true)
    }

    fun sendMessage(sender: CommandSender, message: String, prefix: Boolean) {
        sendMessage(sender, message, null, prefix)
    }

    fun sendMessage(sender: CommandSender, message: String, placeholders: Placeholders?, prefix: Boolean) {
        if (sender is Player) {
            sendMessage(sender, message, placeholders, prefix)
        } else if (sender is ConsoleCommandSender) {
            sendMessage(sender, message, placeholders, prefix)
        } else {
            plugin.logger.warning("Unsupported CommandSender type: ${sender.javaClass.simpleName}")
        }
    }

    fun formatMessage(message: String): String {
        return formatMessage(message, null, true)
    }

    fun formatMessage(message: String, placeholders: Placeholders?): String {
        return formatMessage(message, placeholders, true)
    }

    fun formatMessage(message: String, prefix: Boolean): String {
        return formatMessage(message, null, prefix)
    }

    fun formatMessage(message: String, placeholders: Placeholders?, prefix: Boolean): String {
        val component = buildComponent(message, placeholders, prefix)
        return legacySerializer.serialize(component)
    }

    fun formatComponent(message: String): Component {
        return formatComponent(message, null, false)
    }

    fun formatComponent(message: String, placeholders: Placeholders?): Component {
        return formatComponent(message, placeholders, false)
    }

    fun formatComponent(message: String, prefix: Boolean): Component {
        return formatComponent(message, null, prefix)
    }

    fun formatComponent(message: String, placeholders: Placeholders?, prefix: Boolean): Component {
        return buildComponent(message, placeholders, prefix)
    }

    fun sendActionBar(player: Player, message: String) {
        sendActionBar(player, message, null)
    }

    fun sendActionBar(player: Player, message: String, placeholders: Placeholders?) {
        val component = buildComponent(message, placeholders, false)
        player.sendActionBar(component)
    }

    fun sendActionBarFromConfig(player: Player, path: String, config: ConfigManager? = null) {
        sendActionBarFromConfig(player, path, null, config)
    }

    fun sendActionBarFromConfig(player: Player, path: String, placeholders: Placeholders?, config: ConfigManager? = null) {
        val message = getMessage(path, config) ?: return
        if (message.isEmpty()) return

        val actionPrefix = getMessage("prefix-action", config) ?: ""
        val fullMessage = if (actionPrefix.isNotEmpty()) "$actionPrefix$message" else message

        sendActionBar(player, fullMessage, placeholders)
    }

    fun sendTitle(
        player: Player,
        title: String,
        subtitle: String = "",
        fadeIn: Int = 10,
        stay: Int = 70,
        fadeOut: Int = 20,
        placeholders: Placeholders? = null
    ) {
        val processedTitle = buildComponent(title, placeholders, false)
        val processedSubtitle = buildComponent(subtitle, placeholders, false)

        player.showTitle(
            Title.title(
                processedTitle,
                processedSubtitle,
                Title.Times.times(
                    Duration.ofMillis(fadeIn * 50L),
                    Duration.ofMillis(stay * 50L),
                    Duration.ofMillis(fadeOut * 50L)
                )
            )
        )
    }

    fun broadcast(message: String, permission: String? = null) {
        broadcast(message, null, true, permission)
    }

    fun broadcast(message: String, placeholders: Placeholders?, permission: String? = null) {
        broadcast(message, placeholders, true, permission)
    }

    fun broadcast(message: String, prefix: Boolean, permission: String? = null) {
        broadcast(message, null, prefix, permission)
    }

    fun broadcast(message: String, placeholders: Placeholders?, prefix: Boolean, permission: String? = null) {
        val component = buildComponent(message, placeholders, prefix)

        Bukkit.getOnlinePlayers()
            .filter { permission == null || it.hasPermission(permission) }
            .forEach { it.sendMessage(component) }
    }

    fun getMessage(path: String, config: ConfigManager? = null): String? {
        return (config ?: messagesConfig)?.getConfig()?.getString(path)
    }

    fun sendMessageFromConfig(actor: BukkitCommandActor, path: String, config: ConfigManager? = null) {
        sendMessageFromConfig(actor, path, null, true, config)
    }

    fun sendMessageFromConfig(actor: BukkitCommandActor, path: String, placeholders: Placeholders?, config: ConfigManager? = null) {
        sendMessageFromConfig(actor, path, placeholders, true, config)
    }

    fun sendMessageFromConfig(actor: BukkitCommandActor, path: String, prefix: Boolean, config: ConfigManager? = null) {
        sendMessageFromConfig(actor, path, null, prefix, config)
    }

    fun sendMessageFromConfig(
        actor: BukkitCommandActor,
        path: String,
        placeholders: Placeholders?,
        prefix: Boolean,
        config: ConfigManager? = null
    ) {
        val message = getMessage(path, config) ?: run {
            plugin.logger.warning("Message not found at path: $path")
            return
        }
        if (message.isEmpty()) return
        sendMessage(actor, message, placeholders, prefix)
    }

    fun sendMessageFromConfig(player: Player, path: String, config: ConfigManager? = null) {
        sendMessageFromConfig(player, path, null, true, config)
    }

    fun sendMessageFromConfig(player: Player, path: String, placeholders: Placeholders?, config: ConfigManager? = null) {
        sendMessageFromConfig(player, path, placeholders, true, config)
    }

    fun sendMessageFromConfig(player: Player, path: String, prefix: Boolean, config: ConfigManager? = null) {
        sendMessageFromConfig(player, path, null, prefix, config)
    }

    fun sendMessageFromConfig(
        player: Player,
        path: String,
        placeholders: Placeholders?,
        prefix: Boolean,
        config: ConfigManager? = null
    ) {
        val message = getMessage(path, config) ?: run {
            plugin.logger.warning("Message not found at path: $path")
            return
        }
        if (message.isEmpty()) return
        sendMessage(player, message, placeholders, prefix)
        val actionPath = "$path-action"
        val actionMessage = getMessage(actionPath, config)
        if (actionMessage != null && actionMessage.isNotEmpty()) {
            sendActionBar(player, actionMessage, placeholders)
        }
    }

    fun sendMessageFromConfig(sender: CommandSender, path: String, config: ConfigManager? = null) {
        sendMessageFromConfig(sender, path, null, true, config)
    }

    fun sendMessageFromConfig(sender: CommandSender, path: String, placeholders: Placeholders?, config: ConfigManager? = null) {
        sendMessageFromConfig(sender, path, placeholders, true, config)
    }

    fun sendMessageFromConfig(sender: CommandSender, path: String, prefix: Boolean, config: ConfigManager? = null) {
        sendMessageFromConfig(sender, path, null, prefix, config)
    }

    fun sendMessageFromConfig(sender: CommandSender, path: String, placeholders: Placeholders?, prefix: Boolean, config: ConfigManager? = null) {
        val message = getMessage(path, config) ?: run {
            plugin.logger.warning("Message not found at path: $path")
            return
        }
        if (message.isEmpty()) return
        sendMessage(sender, message, placeholders, prefix)
    }

    fun formatMessageFromConfig(path: String, config: ConfigManager? = null): String {
        return formatMessageFromConfig(path, null, true, config)
    }

    fun formatMessageFromConfig(path: String, placeholders: Placeholders?, config: ConfigManager? = null): String {
        return formatMessageFromConfig(path, placeholders, true, config)
    }

    fun formatMessageFromConfig(path: String, prefix: Boolean, config: ConfigManager? = null): String {
        return formatMessageFromConfig(path, null, prefix, config)
    }

    fun formatMessageFromConfig(
        path: String,
        placeholders: Placeholders?,
        prefix: Boolean,
        config: ConfigManager? = null
    ): String {
        val message = getMessage(path, config) ?: run {
            plugin.logger.warning("Message not found at path: $path")
            return ""
        }
        return formatMessage(message, placeholders, prefix)
    }

    private fun buildComponent(message: String, placeholders: Placeholders?, prefix: Boolean): Component {
        var processed = processText(message)

        if (prefix) {
            val prefixText = getPrefix()
            if (prefixText.isNotEmpty()) {
                val processedPrefix = processText(prefixText)
                processed = "$processedPrefix$processed"
            }
        }

        if (placeholders != null) {
            processed = placeholders.replace(processed)
        }

        return miniMessage.deserialize(processed).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
    }

    fun processText(text: String): String {
        var processed = text
        processed = convertHexColors(processed)
        processed = convertLegacyColors(processed)
        return processed
    }

    private fun getPrefix(): String {
        return try {
            messagesConfig?.getConfig()?.getString("prefix") ?: ""
        } catch (e: Exception) {
            plugin.logger.warning("Failed to get prefix: ${e.message}")
            ""
        }
    }

    private fun convertHexColors(text: String): String {
        val matcher = HEX_PATTERN.matcher(text)
        return matcher.replaceAll("<#$1>")
    }

    private fun convertLegacyColors(text: String): String = text
        .replace("&0", "<black>")
        .replace("&1", "<dark_blue>")
        .replace("&2", "<dark_green>")
        .replace("&3", "<dark_aqua>")
        .replace("&4", "<dark_red>")
        .replace("&5", "<dark_purple>")
        .replace("&6", "<gold>")
        .replace("&7", "<gray>")
        .replace("&8", "<dark_gray>")
        .replace("&9", "<blue>")
        .replace("&a", "<green>")
        .replace("&b", "<aqua>")
        .replace("&c", "<red>")
        .replace("&d", "<light_purple>")
        .replace("&e", "<yellow>")
        .replace("&f", "<white>")
        .replace("&k", "<obfuscated>")
        .replace("&l", "<bold>")
        .replace("&m", "<strikethrough>")
        .replace("&n", "<underlined>")
        .replace("&o", "<italic>")
        .replace("&r", "<reset>")

    fun reloadMessages() {
        try {
            messagesConfig.reloadConfig()
            plugin.logger.info("Message configurations reloaded")
        } catch (e: Exception) {
            plugin.logger.severe("Failed to reload message configurations: ${e.message}")
        }
    }

    fun reload() {
        messagesConfig.reloadConfig()
    }

    private fun cleanup() {
        plugin.logger.info("MessageManager cleaned up successfully")
    }

    fun processColors(message: String): Component {
        return processColors(message, null)
    }

    fun processColors(message: String, placeholders: Placeholders?): Component {
        var processed = message
        if (placeholders != null) {
            processed = placeholders.replace(processed)
        }
        return legacySerializer.deserialize(processed).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
    }
}