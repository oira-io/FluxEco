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

package io.oira.fluxeco.core.redis

import io.oira.fluxeco.core.FluxEco
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import redis.clients.jedis.JedisPool
import java.util.*

@Serializable
data class PlayerJoinMessage(
    val uuid: String,
    val name: String,
    val serverId: String
)

@Serializable
data class PlayerQuitMessage(
    val uuid: String,
    val serverId: String
)

@Serializable
data class PaymentNotificationMessage(
    val targetUuid: String,
    val senderName: String,
    val amount: Double,
    val formattedAmount: String
)

@Serializable
data class EconomyNotificationMessage(
    val targetUuid: String,
    val messageKey: String,
    val placeholders: Map<String, String>
)

class RedisPublisher(private val jedisPool: JedisPool) {
    private val plugin: FluxEco = FluxEco.instance
    private val json = Json { ignoreUnknownKeys = true }

    fun publishPlayerJoin(uuid: UUID, name: String, serverId: String) {
        try {
            val message = PlayerJoinMessage(uuid.toString(), name, serverId)
            val jsonMessage = json.encodeToString(message)

            jedisPool.resource?.use { jedis ->
                jedis.publish(RedisChannels.PLAYER_JOIN.channelName, jsonMessage)
            }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to publish player join event: ${e.message}")
        }
    }

    fun publishPlayerQuit(uuid: UUID, serverId: String) {
        try {
            val message = PlayerQuitMessage(uuid.toString(), serverId)
            val jsonMessage = json.encodeToString(message)

            jedisPool.resource?.use { jedis ->
                jedis.publish(RedisChannels.PLAYER_QUIT.channelName, jsonMessage)
            }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to publish player quit event: ${e.message}")
        }
    }

    fun publishPaymentNotification(targetUuid: UUID, senderName: String, amount: Double, formattedAmount: String) {
        try {
            val message = PaymentNotificationMessage(
                targetUuid.toString(),
                senderName,
                amount,
                formattedAmount
            )
            val jsonMessage = json.encodeToString(message)

            jedisPool.resource?.use { jedis ->
                jedis.publish(RedisChannels.PAYMENT_NOTIFICATION.channelName, jsonMessage)
            }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to publish payment notification: ${e.message}")
        }
    }

    fun publishEconomyNotification(targetUuid: UUID, messageKey: String, placeholders: Map<String, String>) {
        try {
            val message = EconomyNotificationMessage(
                targetUuid.toString(),
                messageKey,
                placeholders
            )
            val jsonMessage = json.encodeToString(message)

            jedisPool.resource?.use { jedis ->
                jedis.publish(RedisChannels.ECONOMY_NOTIFICATION.channelName, jsonMessage)
            }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to publish economy notification: ${e.message}")
        }
    }
}