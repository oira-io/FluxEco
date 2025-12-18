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

package io.oira.fluxeco.api.economy

import io.oira.fluxeco.api.model.Balance
import java.util.*
import java.util.concurrent.CompletableFuture

interface IEconomyManager {

    fun getBalance(uuid: UUID): Double

    fun getBalanceAsync(uuid: UUID): CompletableFuture<Double>

    fun setBalance(uuid: UUID, amount: Double): Int

    fun setBalanceAsync(uuid: UUID, amount: Double): CompletableFuture<Int>

    fun addBalance(uuid: UUID, amount: Double): Int

    fun addBalanceAsync(uuid: UUID, amount: Double): CompletableFuture<Int>

    fun removeBalance(uuid: UUID, amount: Double): Int

    fun removeBalanceAsync(uuid: UUID, amount: Double): CompletableFuture<Int>

    fun hasBalance(uuid: UUID, amount: Double): Boolean

    fun getTopBalances(limit: Int = 10): List<Balance>

    fun getTopBalancesAsync(limit: Int = 10): CompletableFuture<List<Balance>>

    fun getCurrencyName(): String

    fun getCurrencyNamePlural(): String

    fun getStartingBalance(): Double

    fun formatBalance(amount: Double): String
}

