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

package io.oira.fluxeco.core.listener

import com.google.gson.JsonParser
import io.oira.fluxeco.core.data.manager.BalancesDataManager
import io.oira.fluxeco.core.data.manager.PlayerProfileManager
import io.oira.fluxeco.core.manager.EconomyManager
import io.oira.fluxeco.core.redis.RedisManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import java.util.*

class PlayerJoinListener : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val uuid = player.uniqueId
        val name = player.name

        val gameProfile = player.playerProfile
        val properties = gameProfile.properties
        val texturesProperty = properties.find { it.name == "textures" }

        var skinUrl: String? = null
        var capeUrl: String? = null

        if (texturesProperty != null) {
            try {
                val decoded = String(Base64.getDecoder().decode(texturesProperty.value))
                val json = JsonParser.parseString(decoded).asJsonObject
                val textures = json.getAsJsonObject("textures")
                skinUrl = textures.getAsJsonObject("SKIN")?.get("url")?.asString
                capeUrl = textures.getAsJsonObject("CAPE")?.get("url")?.asString
            } catch (_: Exception) {
            }
        }

        PlayerProfileManager.setProfile(uuid, name, skinUrl, capeUrl)

        if (BalancesDataManager.getBalance(uuid) == null) {
            EconomyManager.createBalance(uuid)
        }

        if (RedisManager.isEnabled) {
            RedisManager.getCache()?.addPlayer(uuid, name)
            RedisManager.getPublisher()?.publishPlayerJoin(uuid, name, RedisManager.serverId)
        }
    }
}
