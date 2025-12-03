package io.oira.fluxeco.core.redis

import io.oira.fluxeco.FluxEco
import io.oira.fluxeco.core.manager.ConfigManager
import io.oira.fluxeco.core.manager.MessageManager
import io.oira.fluxeco.core.manager.SettingsManager
import io.oira.fluxeco.core.util.Placeholders
import kotlinx.serialization.json.Json
import org.bukkit.Bukkit
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPubSub
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class RedisListener(private val jedisPool: JedisPool, private val cache: RedisCache) {
    private val plugin: FluxEco = FluxEco.instance
    private val json = Json { ignoreUnknownKeys = true }
    private val messageManager: MessageManager = MessageManager.getInstance()
    private val configManager = ConfigManager(plugin, "messages.yml")
    private var listenerThread: Thread? = null
    private val isRunning = AtomicBoolean(false)
    private var pubSub: JedisPubSub? = null

    fun start() {
        if (isRunning.get()) {
            plugin.logger.warning("Redis listener is already running")
            return
        }

        isRunning.set(true)

        pubSub = object : JedisPubSub() {
            override fun onMessage(channel: String, message: String) {
                handleMessage(channel, message)
            }

            override fun onSubscribe(channel: String, subscribedChannels: Int) {
                plugin.logger.info("Subscribed to Redis channel: $channel")
            }

            override fun onUnsubscribe(channel: String, subscribedChannels: Int) {
                plugin.logger.info("Unsubscribed from Redis channel: $channel")
            }
        }

        listenerThread = Thread({
            try {
                jedisPool.resource?.use { jedis ->
                    jedis.subscribe(
                        pubSub,
                        RedisChannels.PLAYER_JOIN.channelName,
                        RedisChannels.PLAYER_QUIT.channelName,
                        RedisChannels.PAYMENT_NOTIFICATION.channelName,
                        RedisChannels.ECONOMY_NOTIFICATION.channelName
                    )
                }
            } catch (e: Exception) {
                if (isRunning.get()) {
                    plugin.logger.severe("Redis listener error: ${e.message}")
                    e.printStackTrace()
                }
            }
        }, "FluxEco-Redis-Listener").apply {
            isDaemon = true
            start()
        }

        plugin.logger.info("Redis listener started")
    }

    fun stop() {
        if (!isRunning.get()) {
            return
        }

        isRunning.set(false)

        try {
            pubSub?.unsubscribe()
            listenerThread?.interrupt()
            listenerThread?.join(5000)
        } catch (e: Exception) {
            plugin.logger.warning("Error stopping Redis listener: ${e.message}")
        }

        plugin.logger.info("Redis listener stopped")
    }

    private fun handleMessage(channel: String, message: String) {
        try {
            when (channel) {
                RedisChannels.PLAYER_JOIN.channelName -> handlePlayerJoin(message)
                RedisChannels.PLAYER_QUIT.channelName -> handlePlayerQuit(message)
                RedisChannels.PAYMENT_NOTIFICATION.channelName -> handlePaymentNotification(message)
                RedisChannels.ECONOMY_NOTIFICATION.channelName -> handleEconomyNotification(message)
            }
        } catch (e: Exception) {
            plugin.logger.warning("Error handling Redis message on channel $channel: ${e.message}")
        }
    }

    private fun handlePlayerJoin(message: String) {
        try {
            val data = json.decodeFromString<PlayerJoinMessage>(message)
            val uuid = UUID.fromString(data.uuid)

            if (data.serverId != RedisManager.serverId) {
                cache.updateLocalCache(uuid, data.name, data.serverId)
            }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to handle player join message: ${e.message}")
        }
    }

    private fun handlePlayerQuit(message: String) {
        try {
            val data = json.decodeFromString<PlayerQuitMessage>(message)
            val uuid = UUID.fromString(data.uuid)

            if (data.serverId != RedisManager.serverId) {
                cache.removeFromLocalCache(uuid)
            }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to handle player quit message: ${e.message}")
        }
    }

    private fun handlePaymentNotification(message: String) {
        try {
            val data = json.decodeFromString<PaymentNotificationMessage>(message)
            val targetUuid = UUID.fromString(data.targetUuid)

            val targetPlayer = Bukkit.getPlayer(targetUuid)

            if (targetPlayer != null && targetPlayer.isOnline) {
                if (SettingsManager.getPayAlerts(targetUuid)) {
                    plugin.foliaLib.scheduler.runAtEntity(targetPlayer) {
                        val placeholders = Placeholders()
                            .add("player", data.senderName)
                            .add("amount", data.formattedAmount)

                        messageManager.sendMessageFromConfig(
                            targetPlayer,
                            "pay.receive",
                            placeholders,
                            config = configManager
                        )
                    }
                }
            }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to handle payment notification: ${e.message}")
        }
    }

    private fun handleEconomyNotification(message: String) {
        try {
            val data = json.decodeFromString<EconomyNotificationMessage>(message)
            val targetUuid = UUID.fromString(data.targetUuid)

            val targetPlayer = Bukkit.getPlayer(targetUuid)

            if (targetPlayer != null && targetPlayer.isOnline) {
                plugin.foliaLib.scheduler.runAtEntity(targetPlayer) {
                    val placeholders = Placeholders()
                    data.placeholders.forEach { (key, value) ->
                        placeholders.add(key, value)
                    }


                    messageManager.sendMessageFromConfig(
                        targetPlayer,
                        data.messageKey,
                        placeholders,
                        config = configManager
                    )
                }
            }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to handle economy notification: ${e.message}")
        }
    }
}