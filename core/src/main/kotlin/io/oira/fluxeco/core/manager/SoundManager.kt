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

package io.oira.fluxeco.core.manager

import io.oira.fluxeco.core.FluxEco
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import revxrsal.commands.bukkit.actor.BukkitCommandActor

@Suppress("unused")
class SoundManager private constructor() {

    private val plugin: FluxEco = FluxEco.instance

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
            val bukkitSound = Sound.valueOf(sound.uppercase())
            player.playSound(player, bukkitSound, SoundCategory.MASTER, volume, pitch)
        } catch (e: IllegalArgumentException) {
            plugin.logger.warning("Invalid sound: $sound")
        }
    }

    fun playSoundFromConfig(player: Player, path: String, config: ConfigManager?, volume: Float = 1.0f, pitch: Float = 1.0f) {
        val soundConfig = config ?: return
        val cfg = soundConfig.getConfig()
        if (!cfg.getBoolean("sounds.enabled", true)) return
        val sound = cfg.getString("sounds.$path") ?: return
        if (sound.isEmpty()) return
        playSound(player, sound, volume, pitch)
    }

    fun playDelaySound(player: Player, config: ConfigManager?) {
        playSoundFromConfig(player, "delay", config)
    }

    fun playTeleportSound(player: Player, config: ConfigManager?) {
        playSoundFromConfig(player, "teleport", config)
    }

    fun playErrorSound(player: Player, config: ConfigManager?) {
        playSoundFromConfig(player, "error", config)
    }

    fun playSoundFromConfig(actor: BukkitCommandActor, sound: String, config: ConfigManager) {
        actor.asPlayer()?.let { playSoundFromConfig(it, sound, config) }
    }

    fun playTeleportSound(actor: BukkitCommandActor, config: ConfigManager) {
        actor.asPlayer()?.let { playTeleportSound(it, config) }
    }

    fun playErrorSound(actor: BukkitCommandActor, config: ConfigManager) {
        actor.asPlayer()?.let { playErrorSound(it, config) }
    }
}
