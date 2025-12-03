package io.oira.fluxeco.core.listener

import io.oira.fluxeco.core.redis.RedisManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

class PlayerQuitListener : Listener {

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val uuid = player.uniqueId

        if (RedisManager.isEnabled) {
            RedisManager.getCache()?.removePlayer(uuid)
            RedisManager.getPublisher()?.publishPlayerQuit(uuid, RedisManager.serverId)
        }
    }
}

