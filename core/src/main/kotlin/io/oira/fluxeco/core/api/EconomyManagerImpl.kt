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

package io.oira.fluxeco.core.api

import io.oira.fluxeco.api.economy.IEconomyManager
import io.oira.fluxeco.api.event.BalanceChangeEvent
import io.oira.fluxeco.api.event.BalanceChangedEvent
import io.oira.fluxeco.api.model.Balance
import io.oira.fluxeco.core.manager.ConfigManager
import io.oira.fluxeco.core.manager.EconomyManager
import io.oira.fluxeco.core.util.NumberFormatter
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import java.util.*
import java.util.concurrent.CompletableFuture

class EconomyManagerImpl(
    private val plugin: Plugin,
    private val configManager: ConfigManager
) : IEconomyManager {

    private val currencyName: String = configManager.getConfig().getString("currency.name", "Dollar") ?: "Dollar"
    private val currencyNamePlural: String = configManager.getConfig().getString("currency.name-plural", "Dollars") ?: "Dollars"
    private val startingBalance: Double = configManager.getConfig().getDouble("general.starting-balance", 0.0)

    override fun getBalance(uuid: UUID): Double {
        return EconomyManager.getBalance(uuid)
    }

    override fun getBalanceAsync(uuid: UUID): CompletableFuture<Double> {
        return EconomyManager.getBalanceAsync(uuid)
    }

    override fun setBalance(uuid: UUID, amount: Double): Int {
        val oldBalance = getBalance(uuid)

        val event = BalanceChangeEvent(uuid, oldBalance, amount)
        Bukkit.getPluginManager().callEvent(event)

        if (event.isCancelled) {
            return 0
        }

        val result = EconomyManager.setBalance(uuid, event.newBalance)

        if (result > 0) {
            val changedEvent = BalanceChangedEvent(uuid, oldBalance, event.newBalance)
            Bukkit.getPluginManager().callEvent(changedEvent)
        }

        return result
    }

    override fun setBalanceAsync(uuid: UUID, amount: Double): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync {
            setBalance(uuid, amount)
        }
    }

    override fun addBalance(uuid: UUID, amount: Double): Int {
        val current = getBalance(uuid)
        return setBalance(uuid, current + amount)
    }

    override fun addBalanceAsync(uuid: UUID, amount: Double): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync {
            addBalance(uuid, amount)
        }
    }

    override fun removeBalance(uuid: UUID, amount: Double): Int {
        val current = getBalance(uuid)
        val newAmount = (current - amount).coerceAtLeast(0.0)
        return setBalance(uuid, newAmount)
    }

    override fun removeBalanceAsync(uuid: UUID, amount: Double): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync {
            removeBalance(uuid, amount)
        }
    }

    override fun hasBalance(uuid: UUID, amount: Double): Boolean {
        return EconomyManager.hasBalance(uuid, amount)
    }

    override fun getTopBalances(limit: Int): List<Balance> {
        return EconomyManager.getAllBalances()
            .sortedByDescending { it.balance }
            .take(limit)
            .map { Balance(it.uuid, it.balance) }
    }

    override fun getTopBalancesAsync(limit: Int): CompletableFuture<List<Balance>> {
        return EconomyManager.getAllBalancesAsync()
            .thenApply { balances ->
                balances
                    .sortedByDescending { it.balance }
                    .take(limit)
                    .map { Balance(it.uuid, it.balance) }
            }
    }

    override fun getCurrencyName(): String = currencyName

    override fun getCurrencyNamePlural(): String = currencyNamePlural

    override fun getStartingBalance(): Double = startingBalance

    override fun formatBalance(amount: Double): String {
        val formatted = NumberFormatter.format(amount)
        val currency = if (amount == 1.0) currencyName else currencyNamePlural
        return "$formatted $currency"
    }
}

