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

package io.oira.fluxeco.core.data.manager

import io.oira.fluxeco.core.data.DatabaseManager
import io.oira.fluxeco.core.data.model.PlayerSetting
import io.oira.fluxeco.core.data.mongodb.repository.MongoPlayerSettingRepository
import io.oira.fluxeco.core.data.table.PlayerSettings
import io.oira.fluxeco.core.util.Threads
import org.jetbrains.exposed.sql.replace
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import java.util.concurrent.CompletableFuture

object SettingsDataManager {

    fun getSetting(uuid: UUID): PlayerSetting? = if (DatabaseManager.isMongoDB()) {
        MongoPlayerSettingRepository.getSetting(uuid)
    } else {
        transaction(DatabaseManager.getDatabase()) {
            PlayerSettings.selectAll().where { PlayerSettings.uuid eq uuid.toString() }
                .map {
                    PlayerSetting(
                        uuid = UUID.fromString(it[PlayerSettings.uuid]),
                        togglePayments = it[PlayerSettings.togglePayments],
                        payAlerts = it[PlayerSettings.payAlerts]
                    )
                }
                .singleOrNull()
        }
    }

    fun getSettingAsync(uuid: UUID): CompletableFuture<PlayerSetting?> {
        val future = CompletableFuture<PlayerSetting?>()
        Threads.runAsync {
            try {
                val result = getSetting(uuid)
                future.complete(result)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }
        return future
    }

    fun setTogglePayments(uuid: UUID, togglePayments: Boolean): Int = transaction(DatabaseManager.getDatabase()) {
        val current = getSetting(uuid)
        PlayerSettings.replace {
            it[PlayerSettings.uuid] = uuid.toString()
            it[PlayerSettings.togglePayments] = togglePayments
            it[PlayerSettings.payAlerts] = current?.payAlerts ?: true
        }
        1
    }

    fun setTogglePaymentsAsync(uuid: UUID, togglePayments: Boolean): CompletableFuture<Int> {
        val future = CompletableFuture<Int>()
        Threads.runAsync {
            try {
                val result = setTogglePayments(uuid, togglePayments)
                future.complete(result)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }
        return future
    }

    fun getTogglePayments(uuid: UUID): Boolean {
        return getSetting(uuid)?.togglePayments ?: true
    }

    fun getTogglePaymentsAsync(uuid: UUID): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()
        Threads.runAsync {
            try {
                val result = getTogglePayments(uuid)
                future.complete(result)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }
        return future
    }

    fun setPayAlerts(uuid: UUID, payAlerts: Boolean): Int = if (DatabaseManager.isMongoDB()) {
        MongoPlayerSettingRepository.setPayAlerts(uuid, payAlerts)
    } else {
        transaction(DatabaseManager.getDatabase()) {
            val current = getSetting(uuid)
            PlayerSettings.replace {
                it[PlayerSettings.uuid] = uuid.toString()
                it[PlayerSettings.togglePayments] = current?.togglePayments ?: true
                it[PlayerSettings.payAlerts] = payAlerts
            }
            1
        }
    }

    fun setPayAlertsAsync(uuid: UUID, payAlerts: Boolean): CompletableFuture<Int> {
        val future = CompletableFuture<Int>()
        Threads.runAsync {
            try {
                val result = setPayAlerts(uuid, payAlerts)
                future.complete(result)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }
        return future
    }

    fun getPayAlerts(uuid: UUID): Boolean {
        return getSetting(uuid)?.payAlerts ?: true
    }

    fun getPayAlertsAsync(uuid: UUID): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()
        Threads.runAsync {
            try {
                val result = getPayAlerts(uuid)
                future.complete(result)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }
        return future
    }
}
