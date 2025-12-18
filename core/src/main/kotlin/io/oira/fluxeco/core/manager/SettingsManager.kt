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
import io.oira.fluxeco.core.data.manager.SettingsDataManager
import io.oira.fluxeco.core.util.Threads
import java.util.*
import java.util.concurrent.CompletableFuture

object SettingsManager {

    private val plugin: FluxEco = FluxEco.instance

    fun getTogglePayments(uuid: UUID): Boolean {
        return SettingsDataManager.getTogglePayments(uuid)
    }

    fun getTogglePaymentsAsync(uuid: UUID): CompletableFuture<Boolean> {
        return SettingsDataManager.getTogglePaymentsAsync(uuid)
    }

    fun setTogglePayments(uuid: UUID, togglePayments: Boolean): Int {
        return SettingsDataManager.setTogglePayments(uuid, togglePayments)
    }

    fun setTogglePaymentsAsync(uuid: UUID, togglePayments: Boolean): CompletableFuture<Int> {
        return SettingsDataManager.setTogglePaymentsAsync(uuid, togglePayments)
    }

    fun togglePayments(uuid: UUID): Boolean {
        val current = getTogglePayments(uuid)
        val new = !current
        setTogglePayments(uuid, new)
        return new
    }

    fun togglePaymentsAsync(uuid: UUID): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()
        Threads.runAsync {
            try {
                val result = togglePayments(uuid)
                future.complete(result)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }
        return future
    }

    fun getPayAlerts(uuid: UUID): Boolean {
        return SettingsDataManager.getPayAlerts(uuid)
    }

    fun getPayAlertsAsync(uuid: UUID): CompletableFuture<Boolean> {
        return SettingsDataManager.getPayAlertsAsync(uuid)
    }

    fun setPayAlerts(uuid: UUID, payAlerts: Boolean): Int {
        return SettingsDataManager.setPayAlerts(uuid, payAlerts)
    }

    fun setPayAlertsAsync(uuid: UUID, payAlerts: Boolean): CompletableFuture<Int> {
        return SettingsDataManager.setPayAlertsAsync(uuid, payAlerts)
    }

    fun togglePayAlerts(uuid: UUID): Boolean {
        val current = getPayAlerts(uuid)
        val new = !current
        setPayAlerts(uuid, new)
        return new
    }

    fun togglePayAlertsAsync(uuid: UUID): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()
        Threads.runAsync {
            try {
                val result = togglePayAlerts(uuid)
                future.complete(result)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }
        return future
    }
}
