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

