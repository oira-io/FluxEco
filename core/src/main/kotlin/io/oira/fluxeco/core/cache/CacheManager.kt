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

package io.oira.fluxeco.core.cache

import io.oira.fluxeco.core.FluxEco
import io.oira.fluxeco.core.data.manager.BalancesDataManager
import io.oira.fluxeco.core.data.manager.PlayerProfileManager
import io.oira.fluxeco.core.data.manager.TransactionDataManager
import io.oira.fluxeco.core.data.model.Balance
import io.oira.fluxeco.core.data.model.PlayerProfile
import io.oira.fluxeco.core.data.model.Transaction
import io.oira.fluxeco.core.manager.ConfigManager
import io.oira.fluxeco.core.redis.RedisManager
import io.oira.fluxeco.core.util.Threads
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

object CacheManager {

    private val plugin: FluxEco by lazy { FluxEco.instance }
    private val configManager: ConfigManager by lazy { ConfigManager(plugin, "config.yml") }

    private val baltopCache = ConcurrentHashMap<UUID, Balance>()
    private var sortedBaltopCache: List<Balance> = emptyList()
    private val baltopLock = Any()

    private val playerProfileCache = ConcurrentHashMap<UUID, CachedValue<PlayerProfile?>>()
    private val transactionCache = ConcurrentHashMap<UUID, CachedValue<List<Transaction>>>()
    private val balanceCache = ConcurrentHashMap<UUID, CachedValue<Double>>()

    private val lastBaltopUpdate = AtomicLong(0)
    private val isBaltopRefreshing = AtomicBoolean(false)

    private var baltopRefreshTask: ScheduledFuture<*>? = null
    private var cacheCleanupTask: ScheduledFuture<*>? = null

    private var baltopCacheTtlSeconds: Long = 60
    private var profileCacheTtlSeconds: Long = 300
    private var transactionCacheTtlSeconds: Long = 60
    private var balanceCacheTtlSeconds: Long = 10
    private var maxCachedProfiles: Int = 500
    private var maxCachedTransactions: Int = 100
    private var cacheEnabled: Boolean = true
    private var backgroundRefreshEnabled: Boolean = true

    data class CachedValue<T>(
        val value: T,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isExpired(ttlMs: Long): Boolean = System.currentTimeMillis() - timestamp > ttlMs
    }

    fun init() {
        loadConfiguration()

        if (!cacheEnabled) {
            plugin.logger.info("Cache is disabled in configuration")
            return
        }

        refreshBaltopAsync()

        if (backgroundRefreshEnabled) {
            startBackgroundTasks()
        }

        plugin.logger.info("CacheManager initialized with baltop refresh interval: ${baltopCacheTtlSeconds}s")
    }

    private fun loadConfiguration() {
        val config = configManager.getConfig()

        cacheEnabled = config.getBoolean("cache.enabled", true)
        backgroundRefreshEnabled = config.getBoolean("cache.background-refresh", true)
        baltopCacheTtlSeconds = config.getLong("cache.baltop.ttl", 60)
        profileCacheTtlSeconds = config.getLong("cache.profiles.ttl", 300)
        transactionCacheTtlSeconds = config.getLong("cache.transactions.ttl", 60)
        balanceCacheTtlSeconds = config.getLong("cache.balances.ttl", 10)
        maxCachedProfiles = config.getInt("cache.profiles.max-size", 500)
        maxCachedTransactions = config.getInt("cache.transactions.max-size", 100)
    }

    fun reload() {
        stopBackgroundTasks()
        loadConfiguration()

        if (cacheEnabled && backgroundRefreshEnabled) {
            startBackgroundTasks()
        }

        refreshBaltopAsync()
    }

    private fun startBackgroundTasks() {
        baltopRefreshTask = Threads.scheduleAtFixedRate(
            baltopCacheTtlSeconds,
            baltopCacheTtlSeconds,
            TimeUnit.SECONDS
        ) {
            refreshBaltopAsync()
        }

        cacheCleanupTask = Threads.scheduleAtFixedRate(5, 5, TimeUnit.MINUTES) {
            cleanupExpiredCaches()
        }
    }

    private fun stopBackgroundTasks() {
        baltopRefreshTask?.cancel(false)
        baltopRefreshTask = null
        cacheCleanupTask?.cancel(false)
        cacheCleanupTask = null
    }

    fun shutdown() {
        stopBackgroundTasks()
        clearAllCaches()
    }

    fun getBaltop(): List<Balance> {
        if (!cacheEnabled) {
            return BalancesDataManager.getAllBalances().sortedByDescending { it.balance }
        }

        val now = System.currentTimeMillis()
        val cacheAge = now - lastBaltopUpdate.get()

        if (cacheAge > baltopCacheTtlSeconds * 1000 && !isBaltopRefreshing.get()) {
            refreshBaltopAsync()
        }

        synchronized(baltopLock) {
            return sortedBaltopCache.toList()
        }
    }

    fun getBaltopOrNull(): List<Balance>? {
        synchronized(baltopLock) {
            return if (sortedBaltopCache.isEmpty()) null else sortedBaltopCache.toList()
        }
    }

    fun refreshBaltopAsync() {
        if (!isBaltopRefreshing.compareAndSet(false, true)) {
            return
        }

        Threads.runAsync {
            try {
                val balances = BalancesDataManager.getAllBalances().sortedByDescending { it.balance }

                synchronized(baltopLock) {
                    baltopCache.clear()
                    balances.forEach { baltopCache[it.uuid] = it }
                    sortedBaltopCache = balances
                }

                lastBaltopUpdate.set(System.currentTimeMillis())

                if (RedisManager.isEnabled) {
                    RedisManager.getCache()?.updateBaltopCache(balances)
                }
            } catch (e: Exception) {
                plugin.logger.warning("Failed to refresh baltop cache: ${e.message}")
            } finally {
                isBaltopRefreshing.set(false)
            }
        }
    }

    fun refreshBaltopSync(): List<Balance> {
        return try {
            val balances = BalancesDataManager.getAllBalances().sortedByDescending { it.balance }

            synchronized(baltopLock) {
                baltopCache.clear()
                balances.forEach { baltopCache[it.uuid] = it }
                sortedBaltopCache = balances
            }

            lastBaltopUpdate.set(System.currentTimeMillis())

            if (RedisManager.isEnabled) {
                RedisManager.getCache()?.updateBaltopCache(balances)
            }

            balances
        } catch (e: Exception) {
            plugin.logger.warning("Failed to refresh baltop cache: ${e.message}")
            emptyList()
        }
    }

    fun isBaltopStale(): Boolean {
        return System.currentTimeMillis() - lastBaltopUpdate.get() > baltopCacheTtlSeconds * 1000
    }

    fun invalidateBaltop() {
        lastBaltopUpdate.set(0)
    }

    fun getPlayerProfile(uuid: UUID): PlayerProfile? {
        if (!cacheEnabled) {
            return PlayerProfileManager.getProfile(uuid)
        }

        val cached = playerProfileCache[uuid]
        if (cached != null && !cached.isExpired(profileCacheTtlSeconds * 1000)) {
            return cached.value
        }

        val profile = PlayerProfileManager.getProfile(uuid)
        cachePlayerProfile(uuid, profile)
        return profile
    }

    fun getPlayerProfileAsync(uuid: UUID, callback: (PlayerProfile?) -> Unit) {
        val cached = playerProfileCache[uuid]
        if (cached != null && !cached.isExpired(profileCacheTtlSeconds * 1000)) {
            callback(cached.value)
            return
        }

        Threads.runAsync {
            val profile = PlayerProfileManager.getProfile(uuid)
            cachePlayerProfile(uuid, profile)
            Threads.runSync { callback(profile) }
        }
    }

    fun getSkinUrl(uuid: UUID): String? {
        return getPlayerProfile(uuid)?.skinUrl
    }

    fun cachePlayerProfile(uuid: UUID, profile: PlayerProfile?) {
        if (!cacheEnabled) return

        if (playerProfileCache.size >= maxCachedProfiles) {
            evictOldestProfiles(maxCachedProfiles / 4)
        }

        playerProfileCache[uuid] = CachedValue(profile)
    }

    fun invalidatePlayerProfile(uuid: UUID) {
        playerProfileCache.remove(uuid)
    }

    private fun evictOldestProfiles(count: Int) {
        playerProfileCache.entries
            .sortedBy { it.value.timestamp }
            .take(count)
            .forEach { playerProfileCache.remove(it.key) }
    }

    fun getTransactions(uuid: UUID): List<Transaction> {
        if (!cacheEnabled) {
            return TransactionDataManager.getTransactions(uuid)
        }

        val cached = transactionCache[uuid]
        if (cached != null && !cached.isExpired(transactionCacheTtlSeconds * 1000)) {
            return cached.value
        }

        val transactions = TransactionDataManager.getTransactions(uuid)
        cacheTransactions(uuid, transactions)
        return transactions
    }

    fun getTransactionsAsync(uuid: UUID, callback: (List<Transaction>) -> Unit) {
        val cached = transactionCache[uuid]
        if (cached != null && !cached.isExpired(transactionCacheTtlSeconds * 1000)) {
            callback(cached.value)
            return
        }

        Threads.runAsync {
            val transactions = TransactionDataManager.getTransactions(uuid)
            cacheTransactions(uuid, transactions)
            Threads.runSync { callback(transactions) }
        }
    }

    fun cacheTransactions(uuid: UUID, transactions: List<Transaction>) {
        if (!cacheEnabled) return

        if (transactionCache.size >= maxCachedTransactions) {
            evictOldestTransactions(maxCachedTransactions / 4)
        }

        transactionCache[uuid] = CachedValue(transactions)
    }

    fun invalidateTransactions(uuid: UUID) {
        transactionCache.remove(uuid)
    }

    private fun evictOldestTransactions(count: Int) {
        transactionCache.entries
            .sortedBy { it.value.timestamp }
            .take(count)
            .forEach { transactionCache.remove(it.key) }
    }

    fun getBalance(uuid: UUID): Double {
        if (!cacheEnabled) {
            return BalancesDataManager.getBalance(uuid)?.balance ?: 0.0
        }

        val cached = balanceCache[uuid]
        if (cached != null && !cached.isExpired(balanceCacheTtlSeconds * 1000)) {
            return cached.value
        }

        val baltopBalance = baltopCache[uuid]?.balance
        if (baltopBalance != null) {
            cacheBalance(uuid, baltopBalance)
            return baltopBalance
        }

        val balance = BalancesDataManager.getBalance(uuid)?.balance ?: 0.0
        cacheBalance(uuid, balance)
        return balance
    }

    fun cacheBalance(uuid: UUID, balance: Double) {
        if (!cacheEnabled) return
        balanceCache[uuid] = CachedValue(balance)
    }

    fun invalidateBalance(uuid: UUID) {
        balanceCache.remove(uuid)
        invalidateBaltop()
    }

    fun updateBalance(uuid: UUID, newBalance: Double) {
        balanceCache[uuid] = CachedValue(newBalance)

        synchronized(baltopLock) {
            if (baltopCache.containsKey(uuid)) {
                baltopCache[uuid] = Balance(uuid, newBalance)
                sortedBaltopCache = baltopCache.values.sortedByDescending { it.balance }
            }
        }
    }

    private fun cleanupExpiredCaches() {
        val now = System.currentTimeMillis()

        val expiredProfiles = playerProfileCache.entries
            .filter { it.value.isExpired(profileCacheTtlSeconds * 1000) }
            .map { it.key }
        expiredProfiles.forEach { playerProfileCache.remove(it) }

        val expiredTransactions = transactionCache.entries
            .filter { it.value.isExpired(transactionCacheTtlSeconds * 1000) }
            .map { it.key }
        expiredTransactions.forEach { transactionCache.remove(it) }

        val expiredBalances = balanceCache.entries
            .filter { it.value.isExpired(balanceCacheTtlSeconds * 1000) }
            .map { it.key }
        expiredBalances.forEach { balanceCache.remove(it) }

        if (expiredProfiles.isNotEmpty() || expiredTransactions.isNotEmpty() || expiredBalances.isNotEmpty()) {
            plugin.logger.fine("Cache cleanup: removed ${expiredProfiles.size} profiles, ${expiredTransactions.size} transactions, ${expiredBalances.size} balances")
        }
    }

    fun clearAllCaches() {
        synchronized(baltopLock) {
            baltopCache.clear()
            sortedBaltopCache = emptyList()
        }
        playerProfileCache.clear()
        transactionCache.clear()
        balanceCache.clear()
        lastBaltopUpdate.set(0)
    }

    fun getCacheStats(): Map<String, Any> {
        return mapOf(
            "enabled" to cacheEnabled,
            "baltopSize" to baltopCache.size,
            "baltopAge" to (System.currentTimeMillis() - lastBaltopUpdate.get()),
            "profileCacheSize" to playerProfileCache.size,
            "transactionCacheSize" to transactionCache.size,
            "balanceCacheSize" to balanceCache.size
        )
    }
}

