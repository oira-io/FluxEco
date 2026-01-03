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
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.spigotmc.SpigotConfig.config
import revxrsal.commands.bukkit.actor.BukkitCommandActor

@Suppress("unused")
class SoundManager private constructor() {

    private val plugin: FluxEco = FluxEco.instance
    private val configManager by lazy { ConfigManager(plugin, "sounds.yml") }
    private val cfg get() = configManager.getConfig()

    companion object {
        @Volatile
        private var instance: SoundManager? = null

        fun getInstance(): SoundManager = instance ?: synchronized(this) {
            instance ?: SoundManager().also { instance = it }
        }

        fun resetInstance() {
            instance = null
        }
    }

    fun playSound(player: Player, sound: String, volume: Float = 1.0f, pitch: Float = 1.0f) {
        try {
            val key = NamespacedKey.minecraft(sound)
            val bukkitSound = Registry.SOUNDS.get(key) ?: throw IllegalArgumentException("Sound not found")
            player.playSound(player, bukkitSound, SoundCategory.MASTER, volume, pitch)
        } catch (e: IllegalArgumentException) {
            plugin.logger.warning("Invalid sound: $sound")
        }
    }

    fun playSoundFromConfig(player: Player, path: String, volume: Float = 1.0f, pitch: Float = 1.0f) {
        val sound = cfg.getString(path) ?: return
        if (sound.isEmpty()) return
        playSound(player, sound, volume, pitch)
    }

    fun playSoundFromConfig(player: Player, path: String, config: ConfigManager?, volume: Float = 1.0f, pitch: Float = 1.0f) {
        val sound = cfg.getString(path) ?: return
        if (sound.isEmpty()) return
        playSound(player, sound, volume, pitch)
    }

    fun playSoundFromConfig(actor: BukkitCommandActor, sound: String, config: ConfigManager) {
        actor.asPlayer()?.let { playSoundFromConfig(it, sound, config) }
    }

    fun playErrorSound(actor: BukkitCommandActor) {
        actor.asPlayer()?.let { playErrorSound(it) }
    }

    fun playErrorSound(player: Player) {
        playSoundFromConfig(player, "generic.error")
    }
}
