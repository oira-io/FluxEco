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
        /**
 * Determines whether the cached value is older than the provided time-to-live.
 *
 * @param ttlMs Time-to-live in milliseconds to compare against the value's timestamp.
 * @return `true` if the value's age is greater than `ttlMs`, `false` otherwise.
 */
fun isExpired(ttlMs: Long): Boolean = System.currentTimeMillis() - timestamp > ttlMs
    }

    /**
     * Initializes the cache subsystem based on the current configuration.
     *
     * Loads configuration values; if caching is disabled it logs that fact and returns.
     * Otherwise it triggers an asynchronous baltop refresh and, if background refresh is enabled, starts scheduled background tasks.
     * Logs the configured baltop refresh interval after initialization.
     */
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

    /**
     * Loads cache-related configuration values from the ConfigManager into the object's internal settings.
     *
     * Reads the following keys (with defaults) and updates corresponding fields:
     * - `cache.enabled` (true) → enables/disables caching
     * - `cache.background-refresh` (true) → enables/disables background refresh tasks
     * - `cache.baltop.ttl` (60) → baltop TTL in seconds
     * - `cache.profiles.ttl` (300) → profile TTL in seconds
     * - `cache.transactions.ttl` (60) → transactions TTL in seconds
     * - `cache.balances.ttl` (10) → balances TTL in seconds
     * - `cache.profiles.max-size` (500) → maximum cached profiles
     * - `cache.transactions.max-size` (100) → maximum cached transactions
     */
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

    /**
     * Reloads cache configuration and restarts background tasks according to the updated settings.
     *
     * Stops any running background tasks, reloads configuration values, restarts background tasks only if caching
     * and background refresh are both enabled, and triggers an asynchronous baltop refresh.
     */
    fun reload() {
        stopBackgroundTasks()
        loadConfiguration()

        if (cacheEnabled && backgroundRefreshEnabled) {
            startBackgroundTasks()
        }

        refreshBaltopAsync()
    }

    /**
     * Schedules recurring background tasks for maintaining caches.
     *
     * Starts a repeating baltop refresh task that runs at the configured `baltopCacheTtlSeconds` interval
     * and assigns its ScheduledFuture to `baltopRefreshTask`. Also starts a cache cleanup task that runs
     * every 5 minutes and assigns its ScheduledFuture to `cacheCleanupTask`.
     */
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

    /**
     * Stops any scheduled background cache tasks and clears their references.
     *
     * Cancels the baltop refresh and cache cleanup scheduled futures without interrupting running tasks, then sets their variables to null.
     */
    private fun stopBackgroundTasks() {
        baltopRefreshTask?.cancel(false)
        baltopRefreshTask = null
        cacheCleanupTask?.cancel(false)
        cacheCleanupTask = null
    }

    /**
     * Stops all background cache tasks and clears every in-memory cache managed by CacheManager.
     *
     * Cancels scheduled refresh and cleanup tasks and resets baltop and other cached state. 
     */
    fun shutdown() {
        stopBackgroundTasks()
        clearAllCaches()
    }

    /**
     * Returns the current "baltop" — list of balances sorted by descending balance.
     *
     * If caching is disabled this fetches and returns a fresh sorted list from the data manager.
     * If caching is enabled and the cached data is older than the configured TTL an asynchronous
     * refresh is triggered while the current cached snapshot is returned immediately.
     *
     * @return A snapshot list of balances sorted in descending order by balance.
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

    /**
     * Retrieve the currently cached sorted baltop list, or null if the cache is empty.
     *
     * @return A snapshot copy of the cached list of balances sorted in descending order, or `null` if no entries are cached.
     */
    fun getBaltopOrNull(): List<Balance>? {
        synchronized(baltopLock) {
            return if (sortedBaltopCache.isEmpty()) null else sortedBaltopCache.toList()
        }
    }

    /**
     * Asynchronously refreshes the cached global balance leaderboard (baltop).
     *
     * Starts a background refresh if no other refresh is currently running; on success it replaces the in-memory
     * baltop entries and sorted list, updates the last refresh timestamp, and pushes the new list to Redis if enabled.
     *
     * This method guarantees at most one concurrent refresh attempt. */
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

    /**
     * Synchronously refreshes the baltop (top balances) cache from the persistent data store and updates
     * the in-memory baltop structures and Redis cache if enabled.
     *
     * This updates the internal baltopCache, sortedBaltopCache, and lastBaltopUpdate timestamp as part of
     * the refresh operation.
     *
     * @return The refreshed list of balances sorted by balance in descending order, or an empty list if the refresh failed.
     */
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

    /**
     * Checks whether the cached baltop is older than its configured time-to-live.
     *
     * @return `true` if the cached baltop age exceeds the configured TTL, `false` otherwise.
     */
    fun isBaltopStale(): Boolean {
        return System.currentTimeMillis() - lastBaltopUpdate.get() > baltopCacheTtlSeconds * 1000
    }

    /**
     * Marks the cached baltop as stale so it will be refreshed on next access.
     *
     * Sets the internal last-update timestamp to zero to force the next baltop retrieval
     * to perform a refresh.
     */
    fun invalidateBaltop() {
        lastBaltopUpdate.set(0)
    }

    /**
     * Retrieves a player's profile, using the cache when enabled and not expired.
     *
     * When caching is enabled, returns the cached profile if present and within the configured TTL;
     * otherwise fetches the profile from PlayerProfileManager and stores it in the cache.
     *
     * @param uuid The player's UUID.
     * @return The player's `PlayerProfile` if available, `null` if no profile exists.
     */
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

    /**
     * Retrieve a player's profile and invoke the provided callback with the result.
     *
     * If a non-expired profile is cached, the callback is invoked immediately with that value.
     * Otherwise the profile is fetched in a background thread, cached, and the callback is invoked on the main thread when ready.
     *
     * @param uuid The UUID of the player whose profile to retrieve.
     * @param callback Function invoked with the `PlayerProfile` if found, or `null` if not; always executed on the main thread.
     */
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

    /**
     * Retrieve the player's skin URL.
     *
     * @param uuid The player's UUID.
     * @return The skin URL if available, `null` otherwise.
     */
    fun getSkinUrl(uuid: UUID): String? {
        return getPlayerProfile(uuid)?.skinUrl
    }

    /**
     * Caches a player's profile under the given UUID, evicting oldest entries when capacity is reached.
     *
     * Stores the provided `profile` (which may be `null` to record absence) in the profile cache and,
     * if the cache size has reached `maxCachedProfiles`, evicts approximately one quarter of the oldest entries before inserting.
     *
     * @param uuid The player's unique identifier used as the cache key.
     * @param profile The player's profile to cache, or `null` to represent a missing profile.
     */
    fun cachePlayerProfile(uuid: UUID, profile: PlayerProfile?) {
        if (!cacheEnabled) return

        if (playerProfileCache.size >= maxCachedProfiles) {
            evictOldestProfiles(maxCachedProfiles / 4)
        }

        playerProfileCache[uuid] = CachedValue(profile)
    }

    /**
     * Remove a player's cached profile from the in-memory player profile cache.
     *
     * @param uuid The player's UUID whose cached profile should be removed.
     */
    fun invalidatePlayerProfile(uuid: UUID) {
        playerProfileCache.remove(uuid)
    }

    /**
     * Removes the oldest `count` entries from the player profile cache.
     *
     * @param count Number of oldest cached profiles to evict.
     */
    private fun evictOldestProfiles(count: Int) {
        playerProfileCache.entries
            .sortedBy { it.value.timestamp }
            .take(count)
            .forEach { playerProfileCache.remove(it.key) }
    }

    /**
     * Retrieves a player's transaction history.
     *
     * @param uuid The player's UUID.
     * @return A list of `Transaction` objects for the player; returns cached entries when available and valid. 
     */
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

    /**
     * Fetches a player's transactions and invokes `callback` with the result, using the cached value when available.
     *
     * If a non-expired cached value exists it is delivered immediately; otherwise transactions are loaded and cached
     * before the callback is invoked.
     *
     * @param uuid The player's UUID whose transactions should be returned.
     * @param callback Function invoked with the resulting list of `Transaction`; called on the main thread.
     */
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

    /**
     * Stores a user's transaction list in the transaction cache and enforces cache size limits.
     *
     * Does nothing if caching is disabled. When the cache has reached its configured maximum size,
     * the oldest entries are evicted (approximately one quarter of the maximum) before inserting the new value.
     *
     * @param uuid The UUID of the player whose transactions are being cached.
     * @param transactions The list of transactions to store in the cache.
     */
    fun cacheTransactions(uuid: UUID, transactions: List<Transaction>) {
        if (!cacheEnabled) return

        if (transactionCache.size >= maxCachedTransactions) {
            evictOldestTransactions(maxCachedTransactions / 4)
        }

        transactionCache[uuid] = CachedValue(transactions)
    }

    /**
     * Removes cached transactions for the given player UUID.
     *
     * @param uuid The UUID of the player whose transaction cache entry will be removed.
     */
    fun invalidateTransactions(uuid: UUID) {
        transactionCache.remove(uuid)
    }

    /**
     * Removes the specified number of oldest entries from the transaction cache.
     *
     * Evicts up to `count` cached transaction lists, choosing entries with the earliest timestamps.
     *
     * @param count The maximum number of entries to remove.
    private fun evictOldestTransactions(count: Int) {
        transactionCache.entries
            .sortedBy { it.value.timestamp }
            .take(count)
            .forEach { transactionCache.remove(it.key) }
    }

    /**
     * Retrieve the balance for the given player UUID, preferring cached values when available.
     *
     * When caching is enabled, returns a non-expired cached balance if present; otherwise
     * falls back to the baltop in-memory cache if it contains a value, and finally queries
     * the BalancesDataManager. When a value is obtained from baltop or the data manager it
     * is stored in the balance cache.
     *
     * @param uuid The player's UUID.
     * @return The player's balance as a Double; `0.0` if no balance is found.
     */
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

    /**
     * Stores a player's balance in the in-memory balance cache.
     *
     * If caching is disabled this is a no-op. The value is wrapped with the current timestamp.
     *
     * @param uuid The player's unique identifier.
     * @param balance The balance value to cache.
     */
    fun cacheBalance(uuid: UUID, balance: Double) {
        if (!cacheEnabled) return
        balanceCache[uuid] = CachedValue(balance)
    }

    /**
     * Removes the cached balance for a player and marks the baltop cache to be refreshed.
     *
     * Removes any cached balance associated with the given player UUID and invalidates the baltop state
     * so the global balance leaderboard will be refreshed on next access.
     *
     * @param uuid The player's UUID whose cached balance should be removed.
     */
    fun invalidateBalance(uuid: UUID) {
        balanceCache.remove(uuid)
        invalidateBaltop()
    }

    /**
     * Update the cached balance for a player and reflect the change in the baltop if present.
     *
     * Caches the provided `newBalance` for `uuid`. If the player exists in the in-memory baltop,
     * updates that baltop entry and re-sorts the cached baltop list in descending order by balance.
     *
     * @param uuid The player's unique identifier whose balance will be updated.
     * @param newBalance The new balance value to store for the player.
     */
    fun updateBalance(uuid: UUID, newBalance: Double) {
        balanceCache[uuid] = CachedValue(newBalance)

        synchronized(baltopLock) {
            if (baltopCache.containsKey(uuid)) {
                baltopCache[uuid] = Balance(uuid, newBalance)
                sortedBaltopCache = baltopCache.values.sortedByDescending { it.balance }
            }
        }
    }

    /**
     * Removes expired entries from the player profile, transaction, and balance caches.
     *
     * Uses the configured TTLs (profileCacheTtlSeconds, transactionCacheTtlSeconds, balanceCacheTtlSeconds)
     * to determine expiration and logs a summary if any entries were removed.
     */
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

    /**
     * Clears all in-memory caches and resets baltop refresh state.
     *
     * Removes all cached baltop entries, player profiles, transactions, and balances,
     * and resets the baltop last-update timestamp so the next access will trigger a refresh.
     */
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

    /**
     * Provides runtime statistics and sizes for the cache subsystem.
     *
     * The returned map contains the following keys:
     * - "enabled": `Boolean` — whether caching is enabled.
     * - "baltopSize": `Int` — number of entries in the baltop cache.
     * - "baltopAge": `Long` — milliseconds since the last baltop refresh.
     * - "profileCacheSize": `Int` — number of cached player profiles.
     * - "transactionCacheSize": `Int` — number of cached transaction lists.
     * - "balanceCacheSize": `Int` — number of cached balances.
     *
     * @return A map of cache statistics keyed by descriptive strings.
     */
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
