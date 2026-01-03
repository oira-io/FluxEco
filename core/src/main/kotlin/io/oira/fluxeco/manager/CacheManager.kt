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

package io.oira.fluxeco.manager

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.oira.fluxeco.api.model.Balance
import io.oira.fluxeco.api.model.Transaction
import io.oira.fluxeco.FluxEco
import io.oira.fluxeco.data.DatabaseManager
import io.oira.fluxeco.data.manager.PlayerProfileManager
import io.oira.fluxeco.data.manager.TransactionDataManager
import io.oira.fluxeco.data.model.PlayerProfile
import io.oira.fluxeco.data.model.PlayerSetting
import io.oira.fluxeco.data.mongodb.repository.MongoBalanceRepository
import io.oira.fluxeco.data.mongodb.repository.MongoPlayerSettingRepository
import io.oira.fluxeco.data.table.Balances
import io.oira.fluxeco.data.table.PlayerSettings
import io.oira.fluxeco.redis.RedisManager
import io.oira.fluxeco.util.Threads
import io.oira.fluxeco.util.normalize
import org.jetbrains.exposed.sql.replace
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

object CacheManager {

    private val plugin: FluxEco by lazy { FluxEco.instance }
    private val configManager: ConfigManager by lazy { ConfigManager(plugin, "config.yml") }

    private lateinit var balanceCache: Cache<UUID, Double>
    private lateinit var profileCache: Cache<UUID, PlayerProfile>
    private lateinit var transactionCache: Cache<UUID, List<Transaction>>
    private lateinit var settingsCache: Cache<UUID, PlayerSetting>

    private val baltopCache = ConcurrentHashMap<UUID, Double>()
    private var sortedBaltop: List<Balance> = emptyList()
    private val baltopLock = Any()
    private val isBaltopRefreshing = AtomicBoolean(false)

    private var baltopRefreshTask: ScheduledFuture<*>? = null
    private var periodicSaveTask: ScheduledFuture<*>? = null

    private var cacheEnabled: Boolean = true
    private var baltopRefreshSeconds: Long = 60
    private var periodicSaveSeconds: Long = 300
    private var maxCachedProfiles: Int = 1000
    private var maxCachedTransactions: Int = 500

    fun init() {
        loadConfiguration()

        if (!cacheEnabled) {
            plugin.logger.info("Cache is disabled in configuration")
            return
        }

        balanceCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .build()

        profileCache = Caffeine.newBuilder()
            .maximumSize(maxCachedProfiles.toLong())
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build()

        transactionCache = Caffeine.newBuilder()
            .maximumSize(maxCachedTransactions.toLong())
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build()

        settingsCache = Caffeine.newBuilder()
            .maximumSize(5000)
            .build()

        startBackgroundTasks()
    }

    private fun loadConfiguration() {
        val config = configManager.getConfig()
        cacheEnabled = config.getBoolean("cache.enabled", true)
        baltopRefreshSeconds = config.getLong("cache.baltop.ttl", 60)
        periodicSaveSeconds = config.getLong("cache.periodic-save-interval", 300)
        maxCachedProfiles = config.getInt("cache.profiles.max-size", 1000)
        maxCachedTransactions = config.getInt("cache.transactions.max-size", 500)
    }

    fun reload() {
        stopBackgroundTasks()
        loadConfiguration()

        if (cacheEnabled) {
            startBackgroundTasks()
        }
    }

    private fun startBackgroundTasks() {
        baltopRefreshTask = Threads.scheduleAtFixedRate(
            baltopRefreshSeconds,
            baltopRefreshSeconds,
            TimeUnit.SECONDS
        ) {
            refreshBaltop()
        }

        periodicSaveTask = Threads.scheduleAtFixedRate(
            periodicSaveSeconds,
            periodicSaveSeconds,
            TimeUnit.SECONDS
        ) {
            saveAllToDatabase()
        }
    }

    private fun stopBackgroundTasks() {
        baltopRefreshTask?.cancel(false)
        baltopRefreshTask = null
        periodicSaveTask?.cancel(false)
        periodicSaveTask = null
    }

    fun shutdown() {
        stopBackgroundTasks()
        saveAllToDatabase()
        clearAllCaches()
        plugin.logger.info("CacheManager shut down, all data saved to database")
    }

    fun onPlayerJoin(uuid: UUID) {
        if (!cacheEnabled) return
        loadBalanceFromDatabase(uuid)
        loadSettingsFromDatabase(uuid)
    }

    fun onPlayerLeave(uuid: UUID) {
        if (!cacheEnabled) return
        saveBalanceForPlayer(uuid)
    }

    fun getBalance(uuid: UUID): Double {
        if (!cacheEnabled) {
            return loadBalanceFromDatabase(uuid)
        }
        return balanceCache.getIfPresent(uuid) ?: loadBalanceFromDatabase(uuid)
    }

    fun getBalanceAsync(uuid: UUID, callback: (Double) -> Unit) {
        Threads.runAsync {
            val balance = getBalance(uuid)
            Threads.runSync { callback(balance) }
        }
    }

    fun hasBalance(uuid: UUID): Boolean {
        return balanceCache.getIfPresent(uuid) != null
    }

    fun setBalance(uuid: UUID, amount: Double) {
        val normalizedAmount = amount.normalize()
        if (cacheEnabled) {
            balanceCache.put(uuid, normalizedAmount)
        } else {
            saveBalanceToDatabase(uuid, normalizedAmount)
        }
    }

    fun setBalanceAsync(uuid: UUID, amount: Double, callback: (() -> Unit)? = null) {
        Threads.runAsync {
            setBalance(uuid, amount)
            callback?.let { Threads.runSync { it() } }
        }
    }

    fun addBalance(uuid: UUID, amount: Double) {
        val current = getBalance(uuid)
        setBalance(uuid, current + amount)
    }

    fun addBalanceAsync(uuid: UUID, amount: Double, callback: ((Double) -> Unit)? = null) {
        Threads.runAsync {
            addBalance(uuid, amount)
            if (callback != null) {
                val newBalance = getBalance(uuid)
                Threads.runSync { callback(newBalance) }
            }
        }
    }

    fun removeBalance(uuid: UUID, amount: Double) {
        val current = getBalance(uuid)
        setBalance(uuid, current - amount)
    }

    fun removeBalanceAsync(uuid: UUID, amount: Double, callback: ((Double) -> Unit)? = null) {
        Threads.runAsync {
            removeBalance(uuid, amount)
            if (callback != null) {
                val newBalance = getBalance(uuid)
                Threads.runSync { callback(newBalance) }
            }
        }
    }

    fun clearBalance(uuid: UUID) {
        balanceCache.invalidate(uuid)
        baltopCache.remove(uuid)
        synchronized(baltopLock) {
            sortedBaltop = sortedBaltop.filter { it.uuid != uuid }
        }
    }

    fun clearBalanceAsync(uuid: UUID, callback: (() -> Unit)? = null) {
        Threads.runAsync {
            clearBalance(uuid)
            callback?.let { Threads.runSync { it() } }
        }
    }

    fun saveBalanceForPlayer(uuid: UUID) {
        val balance = balanceCache.getIfPresent(uuid) ?: return
        saveBalanceToDatabase(uuid, balance)
    }

    fun saveBalanceForPlayerAsync(uuid: UUID, callback: (() -> Unit)? = null) {
        Threads.runAsync {
            saveBalanceForPlayer(uuid)
            callback?.let { Threads.runSync { it() } }
        }
    }

    fun getBaltop(): List<Balance> {
        synchronized(baltopLock) {
            if (sortedBaltop.isEmpty() && !isBaltopRefreshing.get()) {
                refreshBaltop()
            }
            return sortedBaltop.toList()
        }
    }

    fun getBaltopAsync(callback: (List<Balance>) -> Unit) {
        Threads.runAsync {
            val baltop = getBaltop()
            Threads.runSync { callback(baltop) }
        }
    }

    fun refreshBaltop() {
        if (!isBaltopRefreshing.compareAndSet(false, true)) {
            return
        }

        Threads.runAsync {
            try {
                val allBalances = if (DatabaseManager.isMongoDB()) {
                    MongoBalanceRepository.getAllBalances()
                } else {
                    transaction(DatabaseManager.getDatabase()) {
                        Balances.selectAll().map {
                            Balance(
                                uuid = UUID.fromString(it[Balances.uuid]),
                                balance = it[Balances.balance]
                            )
                        }
                    }
                }.sortedByDescending { it.balance }

                synchronized(baltopLock) {
                    baltopCache.clear()
                    allBalances.forEach { baltopCache[it.uuid] = it.balance }
                    sortedBaltop = allBalances
                }

                if (RedisManager.isEnabled) {
                    RedisManager.getCache()?.updateBaltopCache(allBalances)
                }
            } catch (e: Exception) {
                plugin.logger.warning("Failed to refresh baltop: ${e.message}")
                e.printStackTrace()
            } finally {
                isBaltopRefreshing.set(false)
            }
        }
    }

    fun refreshBaltopAsync() {
        refreshBaltop()
    }

    fun isBaltopStale(): Boolean {
        return sortedBaltop.isEmpty()
    }

    fun invalidateBaltop() {
        synchronized(baltopLock) {
            sortedBaltop = emptyList()
        }
    }

    fun getProfile(uuid: UUID): PlayerProfile? {
        if (!cacheEnabled) {
            return PlayerProfileManager.getProfile(uuid)
        }

        return profileCache.getIfPresent(uuid) ?: run {
            val profile = PlayerProfileManager.getProfile(uuid)
            profile?.let { profileCache.put(uuid, it) }
            profile
        }
    }

    fun getProfileAsync(uuid: UUID, callback: (PlayerProfile?) -> Unit) {
        Threads.runAsync {
            val profile = getProfile(uuid)
            Threads.runSync { callback(profile) }
        }
    }

    fun cacheProfile(uuid: UUID, profile: PlayerProfile) {
        if (cacheEnabled) {
            profileCache.put(uuid, profile)
        }
    }

    fun getSkinUrl(uuid: UUID): String? {
        return getProfile(uuid)?.skinUrl
    }

    fun getSettings(uuid: UUID): PlayerSetting? {
        if (!cacheEnabled) {
            return loadSettingsFromDatabase(uuid)
        }

        return settingsCache.getIfPresent(uuid) ?: loadSettingsFromDatabase(uuid)
    }

    fun getSettingsAsync(uuid: UUID, callback: (PlayerSetting?) -> Unit) {
        Threads.runAsync {
            val settings = getSettings(uuid)
            Threads.runSync { callback(settings) }
        }
    }

    fun cacheSettings(uuid: UUID, settings: PlayerSetting) {
        if (cacheEnabled) {
            settingsCache.put(uuid, settings)
        }
    }

    fun getTransactions(uuid: UUID): List<Transaction> {
        if (!cacheEnabled) {
            return TransactionDataManager.getTransactions(uuid)
        }

        return transactionCache.getIfPresent(uuid) ?: run {
            val transactions = TransactionDataManager.getTransactions(uuid)
            transactionCache.put(uuid, transactions)
            transactions
        }
    }

    fun getTransactionsAsync(uuid: UUID, callback: (List<Transaction>) -> Unit) {
        Threads.runAsync {
            val transactions = getTransactions(uuid)
            Threads.runSync { callback(transactions) }
        }
    }

    fun invalidateTransactions(uuid: UUID) {
        transactionCache.invalidate(uuid)
    }

    fun clearAllCaches() {
        balanceCache.invalidateAll()
        profileCache.invalidateAll()
        transactionCache.invalidateAll()
        settingsCache.invalidateAll()

        synchronized(baltopLock) {
            baltopCache.clear()
            sortedBaltop = emptyList()
        }
    }

    private fun loadBalanceFromDatabase(uuid: UUID): Double {
        val balance = if (DatabaseManager.isMongoDB()) {
            MongoBalanceRepository.getBalance(uuid)?.balance ?: 0.0
        } else {
            transaction(DatabaseManager.getDatabase()) {
                Balances.selectAll()
                    .where { Balances.uuid eq uuid.toString() }
                    .map { it[Balances.balance] }
                    .singleOrNull() ?: 0.0
            }
        }

        if (cacheEnabled) {
            balanceCache.put(uuid, balance)
        }

        return balance
    }

    private fun saveBalanceToDatabase(uuid: UUID, amount: Double) {
        if (DatabaseManager.isMongoDB()) {
            MongoBalanceRepository.updateBalance(uuid, amount)
        } else {
            transaction(DatabaseManager.getDatabase()) {
                Balances.replace {
                    it[Balances.uuid] = uuid.toString()
                    it[balance] = amount
                }
            }
        }
    }

    private fun loadSettingsFromDatabase(uuid: UUID): PlayerSetting? {
        val settings = if (DatabaseManager.isMongoDB()) {
            MongoPlayerSettingRepository.getSetting(uuid)
        } else {
            transaction(DatabaseManager.getDatabase()) {
                PlayerSettings.selectAll()
                    .where { PlayerSettings.uuid eq uuid.toString() }
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

        settings?.let { settingsCache.put(uuid, it) }
        return settings
    }

    private fun saveAllToDatabase() {
        if (!cacheEnabled) return

        Threads.runAsync {
            try {
                val balancesToSave = balanceCache.asMap().toMap()

                if (balancesToSave.isEmpty()) {
                    return@runAsync
                }

                if (DatabaseManager.isMongoDB()) {
                    balancesToSave.forEach { (uuid, balance) ->
                        MongoBalanceRepository.updateBalance(uuid, balance)
                    }
                } else {
                    transaction(DatabaseManager.getDatabase()) {
                        balancesToSave.forEach { (uuid, balance) ->
                            Balances.replace {
                                it[Balances.uuid] = uuid.toString()
                                it[Balances.balance] = balance
                            }
                        }
                    }
                }

                plugin.logger.fine("Saved ${balancesToSave.size} balances to database")
            } catch (e: Exception) {
                plugin.logger.severe("Failed to save cached data to database: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}