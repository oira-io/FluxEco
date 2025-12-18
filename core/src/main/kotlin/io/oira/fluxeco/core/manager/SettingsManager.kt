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
