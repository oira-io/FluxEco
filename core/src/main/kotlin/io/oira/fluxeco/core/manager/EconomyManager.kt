package io.oira.fluxeco.core.manager

import io.oira.fluxeco.core.FluxEco
import io.oira.fluxeco.core.cache.CacheManager
import io.oira.fluxeco.core.data.manager.BalancesDataManager
import io.oira.fluxeco.core.data.model.Balance
import io.oira.fluxeco.core.util.Threads
import java.util.*
import java.util.concurrent.CompletableFuture

object EconomyManager {

    private val plugin: FluxEco = FluxEco.instance
    private val configManager = ConfigManager(plugin, "config.yml")

    private val currencyName: String = configManager.getConfig().getString("currency.name", "Dollar") ?: "Dollar"
    private val currencyNamePlural: String = configManager.getConfig().getString("currency.name-plural", "Dollars") ?: "Dollars"
    private val startingBalance: Double = configManager.getConfig().getDouble("general.starting-balance", 0.0)

    fun getBalance(uuid: UUID): Double {
        return BalancesDataManager.getBalance(uuid)?.balance ?: 0.0
    }

    fun getBalanceAsync(uuid: UUID): CompletableFuture<Double> {
        val future = CompletableFuture<Double>()
        Threads.runAsync {
            try {
                val balance = getBalance(uuid)
                future.complete(balance)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }
        return future
    }

    fun setBalance(uuid: UUID, amount: Double): Int {
        val result = BalancesDataManager.updateBalance(uuid, amount)
        CacheManager.updateBalance(uuid, amount)
        return result
    }

    fun setBalanceAsync(uuid: UUID, amount: Double): CompletableFuture<Int> {
        val future = CompletableFuture<Int>()
        Threads.runAsync {
            try {
                val result = setBalance(uuid, amount)
                future.complete(result)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }
        return future
    }

    fun addBalance(uuid: UUID, amount: Double): Int {
        val current = getBalance(uuid)
        return setBalance(uuid, current + amount)
    }

    fun addBalanceAsync(uuid: UUID, amount: Double): CompletableFuture<Int> {
        val future = CompletableFuture<Int>()
        Threads.runAsync {
            try {
                val result = addBalance(uuid, amount)
                future.complete(result)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }
        return future
    }

    fun subtractBalance(uuid: UUID, amount: Double): Boolean {
        val current = getBalance(uuid)
        if (current < amount) return false
        setBalance(uuid, current - amount)
        return true
    }

    fun subtractBalanceAsync(uuid: UUID, amount: Double): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()
        Threads.runAsync {
            try {
                val result = subtractBalance(uuid, amount)
                future.complete(result)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }
        return future
    }

    fun transfer(from: UUID, to: UUID, amount: Double): Boolean {
        if (!subtractBalance(from, amount)) return false
        addBalance(to, amount)
        return true
    }

    fun transferAsync(from: UUID, to: UUID, amount: Double): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()
        Threads.runAsync {
            try {
                val result = transfer(from, to, amount)
                future.complete(result)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }
        return future
    }

    fun hasBalance(uuid: UUID, amount: Double): Boolean {
        return getBalance(uuid) >= amount
    }

    fun hasBalanceAsync(uuid: UUID, amount: Double): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()
        Threads.runAsync {
            try {
                val result = hasBalance(uuid, amount)
                future.complete(result)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }
        return future
    }

    fun getCurrencyName(amount: Double): String {
        return if (amount == 1.0) currencyName else currencyNamePlural
    }

    fun getAllBalances(): List<Balance> {
        return BalancesDataManager.getAllBalances()
    }

    fun getAllBalancesAsync(): CompletableFuture<List<Balance>> {
        val future = CompletableFuture<List<Balance>>()
        Threads.runAsync {
            try {
                val result = getAllBalances()
                future.complete(result)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }
        return future
    }

    fun createBalance(uuid: UUID) {
        BalancesDataManager.createBalance(uuid, startingBalance)
    }

    fun createBalanceAsync(uuid: UUID): CompletableFuture<Unit> {
        val future = CompletableFuture<Unit>()
        Threads.runAsync {
            try {
                createBalance(uuid)
                future.complete(Unit)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }
        return future
    }
}
