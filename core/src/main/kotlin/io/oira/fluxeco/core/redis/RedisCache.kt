package io.oira.fluxeco.core.redis

import io.oira.fluxeco.FluxEco
import io.oira.fluxeco.api.model.BaltopEntry
import io.oira.fluxeco.api.model.PlayerSession
import io.oira.fluxeco.core.data.model.Balance
import redis.clients.jedis.JedisPool
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class RedisCache(private val jedisPool: JedisPool, private val serverId: String) {
    private val plugin: FluxEco = FluxEco.instance
    private val localCache = ConcurrentHashMap<UUID, PlayerSession>()
    private val baltopCache = ConcurrentHashMap<UUID, BaltopEntry>()
    private var lastBaltopUpdate: Long = 0

    companion object {
        private const val PLAYER_KEY_PREFIX = "fluxeco:player:"
        private const val PLAYER_TTL = 300L
        private const val BALTOP_KEY = "fluxeco:baltop"
        private const val BALTOP_CACHE_TTL = 30000L
    }

    fun addPlayer(uuid: UUID, name: String) {
        try {
            val session = PlayerSession(uuid, name, serverId)
            localCache[uuid] = session

            jedisPool.resource?.use { jedis ->
                val key = "$PLAYER_KEY_PREFIX$uuid"
                jedis.hset(key, mapOf(
                    "name" to name,
                    "server" to serverId,
                    "uuid" to uuid.toString()
                ))
                jedis.expire(key, PLAYER_TTL)
            }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to add player to Redis cache: ${e.message}")
        }
    }

    fun removePlayer(uuid: UUID) {
        try {
            localCache.remove(uuid)

            jedisPool.resource?.use { jedis ->
                val key = "$PLAYER_KEY_PREFIX$uuid"
                jedis.del(key)
            }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to remove player from Redis cache: ${e.message}")
        }
    }

    fun getPlayer(uuid: UUID): PlayerSession? {
        localCache[uuid]?.let { return it }

        try {
            jedisPool.resource?.use { jedis ->
                val key = "$PLAYER_KEY_PREFIX$uuid"
                val data = jedis.hgetAll(key)

                if (data.isNotEmpty()) {
                    val session = PlayerSession(
                        uuid = UUID.fromString(data["uuid"]),
                        name = data["name"] ?: return null,
                        serverId = data["server"] ?: return null
                    )
                    localCache[uuid] = session
                    return session
                }
            }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to get player from Redis cache: ${e.message}")
        }

        return null
    }

    fun getAllPlayers(): List<PlayerSession> {
        val players = mutableListOf<PlayerSession>()

        try {
            jedisPool.resource?.use { jedis ->
                val keys = jedis.keys("$PLAYER_KEY_PREFIX*")

                for (key in keys) {
                    val data = jedis.hgetAll(key)
                    if (data.isNotEmpty()) {
                        try {
                            val session = PlayerSession(
                                uuid = UUID.fromString(data["uuid"]),
                                name = data["name"] ?: continue,
                                serverId = data["server"] ?: continue
                            )
                            players.add(session)
                            localCache[session.uuid] = session
                        } catch (e: Exception) {
                        }
                    }
                }
            }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to get all players from Redis cache: ${e.message}")
        }

        return players
    }

    fun getAllPlayerNames(): Set<String> {
        return try {
            getAllPlayers().map { it.name }.toSet()
        } catch (e: Exception) {
            plugin.logger.warning("Failed to get player names from Redis cache: ${e.message}")
            emptySet()
        }
    }

    fun isPlayerOnline(uuid: UUID): Boolean {
        return getPlayer(uuid) != null
    }

    fun updateLocalCache(uuid: UUID, name: String, serverId: String) {
        if (serverId != this.serverId) {
            localCache[uuid] = PlayerSession(uuid, name, serverId)
        }
    }

    fun removeFromLocalCache(uuid: UUID) {
        localCache.remove(uuid)
    }

    fun updateBaltopCache(balances: List<Balance>) {
        try {
            jedisPool.resource?.use { jedis ->
                jedis.del(BALTOP_KEY)

                val scoreMembers = mutableMapOf<String, Double>()
                for (balance in balances) {
                    val member = "${balance.uuid}:${getPlayerNameFromBalance(balance.uuid)}"
                    scoreMembers[member] = balance.balance
                }

                if (scoreMembers.isNotEmpty()) {
                    jedis.zadd(BALTOP_KEY, scoreMembers)
                    jedis.expire(BALTOP_KEY, 300)
                }
            }
            lastBaltopUpdate = System.currentTimeMillis()
        } catch (e: Exception) {
            plugin.logger.warning("Failed to update baltop cache in Redis: ${e.message}")
        }
    }

    fun getBaltopFromCache(): List<BaltopEntry>? {
        if (System.currentTimeMillis() - lastBaltopUpdate < BALTOP_CACHE_TTL && baltopCache.isNotEmpty()) {
            return baltopCache.values.sortedByDescending { it.balance }
        }

        try {
            jedisPool.resource?.use { jedis ->
                val members = jedis.zrevrangeWithScores(BALTOP_KEY, 0, -1)

                if (members.isEmpty()) {
                    return null
                }

                val entries = mutableListOf<BaltopEntry>()
                baltopCache.clear()

                for (tuple in members) {
                    try {
                        val parts = tuple.element.split(":", limit = 2)
                        if (parts.size == 2) {
                            val uuid = UUID.fromString(parts[0])
                            val name = parts[1]
                            val balance = tuple.score

                            val entry = BaltopEntry(uuid, name, balance)
                            entries.add(entry)
                            baltopCache[uuid] = entry
                        }
                    } catch (e: Exception) {
                    }
                }

                lastBaltopUpdate = System.currentTimeMillis()
                return entries
            }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to get baltop from Redis cache: ${e.message}")
        }

        return null
    }

    fun shouldRefreshBaltop(): Boolean {
        return System.currentTimeMillis() - lastBaltopUpdate > BALTOP_CACHE_TTL
    }

    private fun getPlayerNameFromBalance(uuid: UUID): String {
        localCache[uuid]?.let { return it.name }

        val offlinePlayer = org.bukkit.Bukkit.getOfflinePlayer(uuid)
        return offlinePlayer.name ?: "Unknown"
    }

    fun cleanup() {
        try {
            jedisPool.resource?.use { jedis ->
                val keys = jedis.keys("$PLAYER_KEY_PREFIX*")
                for (key in keys) {
                    val data = jedis.hgetAll(key)
                    if (data["server"] == serverId) {
                        jedis.del(key)
                    }
                }
            }
            localCache.clear()
            baltopCache.clear()
        } catch (e: Exception) {
            plugin.logger.warning("Failed to cleanup Redis cache: ${e.message}")
        }
    }
}