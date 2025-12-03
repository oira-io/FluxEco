package io.oira.fluxeco.core.gui.impl

import com.destroystokyo.paper.profile.ProfileProperty
import com.google.gson.JsonObject
import io.oira.fluxeco.core.data.manager.PlayerProfileManager
import io.oira.fluxeco.core.data.model.Balance
import io.oira.fluxeco.core.gui.BaseGUI
import io.oira.fluxeco.core.manager.EconomyManager
import io.oira.fluxeco.core.redis.RedisManager
import io.oira.fluxeco.core.util.Placeholders
import io.oira.fluxeco.core.util.format
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.meta.ItemMeta
import java.util.*

class BaltopGUI : BaseGUI("gui/baltop-ui.yml") {

    private val playerCache = mutableMapOf<UUID, CachedPlayer>()

    private var balances: List<Balance> = emptyList()
    private var lastRefreshTime: Long = 0

    data class CachedPlayer(
        val name: String,
        val offlinePlayer: OfflinePlayer
    )

    init {
        registerActions()
        loadBalances()
        initialize()
    }

    private fun registerActions() {
        registerSimpleAction("previous_page") { player -> handlePreviousPage(player) }
        registerSimpleAction("next_page") { player -> handleNextPage(player) }
        registerSimpleAction("refresh") { player -> handleRefresh(player) }
        registerSimpleAction("search") { player -> handleSearch(player) }
        registerAction("entry") { player, event -> handleEntryClick(player, event) }
    }

    private fun handlePreviousPage(player: Player) {
        if (currentPage > 0) {
            currentPage--
            refreshDynamicItems()
        } else {
            messageManager.sendMessageFromConfig(player, "messages.previous-page-error", configManager)
            soundManager.playSoundFromConfig(player, "error", configManager)
        }
    }

    private fun handleNextPage(player: Player) {
        if (currentPage < getMaxPages() - 1) {
            currentPage++
            refreshDynamicItems()
        } else {
            messageManager.sendMessageFromConfig(player, "messages.next-page-error", configManager)
            soundManager.playSoundFromConfig(player, "error", configManager)
        }
    }

    private fun handleRefresh(player: Player) {
        val currentTime = System.currentTimeMillis()
        val refreshCooldown = getRefreshCooldown()

        if (currentTime - lastRefreshTime < refreshCooldown) {
            sendCooldownMessage(player, refreshCooldown, currentTime)
            return
        }

        lastRefreshTime = currentTime
        playerCache.clear()
        loadBalances()
        currentPage = 0
        refreshDynamicItems()
        messageManager.sendMessageFromConfig(player, "messages.refresh-success", configManager)
    }

    private fun sendCooldownMessage(player: Player, refreshCooldown: Long, currentTime: Long) {
        val remaining = ((refreshCooldown - (currentTime - lastRefreshTime)) / 1000.0)
        val placeholders = Placeholders().add("remaining", String.format("%.1f", remaining))
        messageManager.sendMessageFromConfig(player, "messages.refresh-cooldown", placeholders, configManager)
        soundManager.playSoundFromConfig(player, "error", configManager)
    }

    private fun handleSearch(player: Player) {
        BaltopSignGUI(this).open(player)
    }

    private fun handleEntryClick(player: Player, event: InventoryClickEvent) {
        val item = event.currentItem ?: return
        val meta = item.itemMeta ?: return
        val playerUuid = meta.persistentDataContainer.get(
            NamespacedKey(plugin, "player_uuid"),
            PersistentDataType.STRING
        ) ?: return

        try {
            val uuid = UUID.fromString(playerUuid)
            val balance = balances.find { it.uuid == uuid }

            if (balance != null) {
                sendEntryClickMessage(player, balance)
            } else {
                messageManager.sendMessageFromConfig(player, "messages.player-data-not-found", configManager)
            }
        } catch (_: IllegalArgumentException) {
            plugin.logger.warning("Invalid UUID in baltop entry: $playerUuid")
        }
    }

    private fun sendEntryClickMessage(player: Player, balance: Balance) {
        val cachedPlayer = getCachedPlayer(balance.uuid)
        val rank = balances.indexOf(balance) + 1

        val placeholders = Placeholders()
            .add("player", cachedPlayer.name)
            .add("balance", balance.balance.format())
            .add("rank", rank.toString())

        messageManager.sendMessageFromConfig(player, "messages.entry-click", placeholders, configManager)
    }

    private fun getRefreshCooldown(): Long {
        return (config.getConfigurationSection("items.refresh")?.getLong("cooldown", 5) ?: 5) * 1000
    }

    private fun loadBalances() {
        balances = try {
            EconomyManager.getAllBalances().sortedByDescending { it.balance }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to load balances: ${e.message}")
            emptyList()
        }
    }

    private fun getCachedPlayer(uuid: UUID): CachedPlayer {
        return playerCache.getOrPut(uuid) {
            val offlinePlayer = Bukkit.getOfflinePlayer(uuid)
            val name = offlinePlayer.name ?: "Unknown"
            CachedPlayer(name, offlinePlayer)
        }
    }

    private fun getPlayerRank(player: Player): Int {
        val balance = balances.find { it.uuid == player.uniqueId }
        return if (balance != null) balances.indexOf(balance) + 1 else 0
    }

    private fun createPlayerItem(player: Player): ItemStack? {
        val itemConfig = config.getConfigurationSection("items.player") ?: return null

        val rank = getPlayerRank(player)
        val playerBalance = balances.find { it.uuid == player.uniqueId }?.balance ?: 0.0

        val placeholders = Placeholders()
            .add("player", player.name)
            .add("user-rank", rank.toString())
            .add("balance", playerBalance.format())

        val item = createItemFromConfig(itemConfig, "player", placeholders) ?: return null

        if (item.type == Material.PLAYER_HEAD && item.itemMeta is SkullMeta) {
            val meta = item.itemMeta as SkullMeta
            val cachedPlayer = CachedPlayer(player.name, player)
            setPlayerHead(meta, player.uniqueId, cachedPlayer)
            item.itemMeta = meta
        }

        return item
    }

    override fun getEntryData(): List<Any> = balances

    override fun filterData(data: List<Any>): List<Any> {
        if (searchQuery.isEmpty()) return data
        return data.filterIsInstance<Balance>().filter { balance ->
            getCachedPlayer(balance.uuid).name.contains(searchQuery, ignoreCase = true)
        }
    }

    override fun createEntryItem(data: Any, index: Int): ItemStack? {
        if (data !is Balance) return null
        val entryConfig = config.getConfigurationSection("items.entry") ?: return null
        val globalIndex = currentPage * entriesPerPage + index
        return createBalanceEntry(data, globalIndex, entryConfig)
    }

    private fun createBalanceEntry(
        balance: Balance,
        globalIndex: Int,
        entryConfig: ConfigurationSection
    ): ItemStack {
        val cachedPlayer = getCachedPlayer(balance.uuid)
        val rank = globalIndex + 1
        val placeholders = createBalancePlaceholders(cachedPlayer, balance, rank)

        val material = getMaterialFromConfig(entryConfig)
        val item = ItemStack(material)
        val meta = item.itemMeta ?: return item

        applyBalanceEntryMeta(meta, balance, placeholders, entryConfig, cachedPlayer)

        item.itemMeta = meta
        return item
    }

    private fun createBalancePlaceholders(
        cachedPlayer: CachedPlayer,
        balance: Balance,
        rank: Int
    ): Placeholders {
        return Placeholders()
            .add("player", cachedPlayer.name)
            .add("balance", balance.balance.format())
            .add("rank", rank.toString())
    }

    private fun getMaterialFromConfig(entryConfig: ConfigurationSection): Material {
        val materialName = entryConfig.getString("material", "PLAYER_HEAD") ?: "PLAYER_HEAD"
        return try {
            Material.valueOf(materialName.uppercase())
        } catch (_: IllegalArgumentException) {
            Material.PLAYER_HEAD
        }
    }

    private fun applyBalanceEntryMeta(
        meta: ItemMeta,
        balance: Balance,
        placeholders: Placeholders,
        entryConfig: ConfigurationSection,
        cachedPlayer: CachedPlayer
    ) {
        val nameString = entryConfig.getString("name", "{player}") ?: "{player}"
        meta.displayName(messageManager.processColors(nameString, placeholders))

        val loreList = entryConfig.getStringList("lore")
        val lore = loreList.map { messageManager.processColors(it, placeholders) }
        meta.lore(lore)

        if (meta is SkullMeta) {
            setPlayerHead(meta, balance.uuid, cachedPlayer)
        }

        val container = meta.persistentDataContainer
        container.set(actionKey, PersistentDataType.STRING, "entry")
        container.set(NamespacedKey(plugin, "player_uuid"), PersistentDataType.STRING, balance.uuid.toString())
        container.set(guiIdKey, PersistentDataType.STRING, guiId)
        container.set(soundKey, PersistentDataType.STRING, entryConfig.getString("sound", "none") ?: "none")
    }

    private fun setPlayerHead(skullMeta: SkullMeta, uuid: UUID, cachedPlayer: CachedPlayer) {
        val skinUrl = getSkinUrl(uuid)

        if (skinUrl != null) {
            applyCustomSkin(skullMeta, skinUrl, cachedPlayer)
        } else {
            skullMeta.owningPlayer = cachedPlayer.offlinePlayer
        }
    }

    private fun getSkinUrl(uuid: UUID): String? {
        return try {
            PlayerProfileManager.getSkinUrl(uuid)
        } catch (_: Exception) {
            null
        }
    }

    private fun applyCustomSkin(skullMeta: SkullMeta, skinUrl: String, cachedPlayer: CachedPlayer) {
        try {
            val texturesJson = createTexturesJson(skinUrl)
            val encoded = Base64.getEncoder().encodeToString(texturesJson.toString().toByteArray())
            val playerProfile = Bukkit.createProfile(UUID.randomUUID(), cachedPlayer.name)
            val property = ProfileProperty("textures", encoded)
            playerProfile.setProperty(property)
            skullMeta.playerProfile = playerProfile
        } catch (e: Exception) {
            plugin.logger.warning("Failed to set custom skin for ${cachedPlayer.name}: ${e.message}")
            skullMeta.owningPlayer = cachedPlayer.offlinePlayer
        }
    }

    private fun createTexturesJson(skinUrl: String): JsonObject {
        val texturesJson = JsonObject()
        val skinJson = JsonObject()
        skinJson.addProperty("url", skinUrl)
        texturesJson.add("SKIN", skinJson)

        val json = JsonObject()
        json.add("textures", texturesJson)
        return json
    }

    override fun getMaxPages(): Int {
        val filtered = filterData(balances)
        return if (filtered.isEmpty()) 1 else ((filtered.size - 1) / entriesPerPage) + 1
    }

    override fun open(player: Player) {
        if (shouldRefreshBalances()) {
            if (RedisManager.isEnabled) {
                val redisCache = RedisManager.getCache()
                if (redisCache != null && !redisCache.shouldRefreshBaltop()) {
                    val cachedBaltop = redisCache.getBaltopFromCache()
                    if (cachedBaltop != null) {
                        balances = cachedBaltop.map { entry ->
                            Balance(entry.uuid, entry.balance)
                        }
                    } else {
                        loadBalances()
                    }
                } else {
                    loadBalances()
                }
            } else {
                loadBalances()
            }
            lastRefreshTime = System.currentTimeMillis()
        }

        currentPage = 0
        refreshDynamicItems()

        val playerItem = createPlayerItem(player)
        if (playerItem != null) {
            inventory.setItem(48, playerItem)
        }

        super.open(player)
    }

    private fun shouldRefreshBalances(): Boolean {
        return balances.isEmpty() || System.currentTimeMillis() - lastRefreshTime > 30000
    }

    override fun onOpen(player: Player) {
        val totalPlayers = balances.size
        val placeholders = Placeholders().add("total", totalPlayers.toString())
        messageManager.sendMessageFromConfig(player, "messages.open", placeholders, configManager)
    }

    override fun reload() {
        loadBalances()
        super.reload()
    }
}