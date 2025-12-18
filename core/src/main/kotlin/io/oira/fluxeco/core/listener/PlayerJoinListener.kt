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
