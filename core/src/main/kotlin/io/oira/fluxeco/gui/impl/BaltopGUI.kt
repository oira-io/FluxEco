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

package io.oira.fluxeco.gui.impl

import com.destroystokyo.paper.profile.ProfileProperty
import com.google.gson.JsonObject
import io.oira.fluxeco.api.model.Balance
import io.oira.fluxeco.gui.BaseGUI
import io.oira.fluxeco.manager.CacheManager
import io.oira.fluxeco.manager.ConfigManager
import io.oira.fluxeco.util.Placeholders
import io.oira.fluxeco.util.Threads
import io.oira.fluxeco.util.format
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.OfflinePlayer
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType
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
            messageManager.sendMessageFromConfig(player, "gui.previous-page-error")
            soundManager.playErrorSound(player)
        }
    }

    private fun handleNextPage(player: Player) {
        if (currentPage < getMaxPages() - 1) {
            currentPage++
            refreshDynamicItems()
        } else {
            messageManager.sendMessageFromConfig(player, "gui.next-page-error")
            soundManager.playErrorSound(player)
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

        CacheManager.invalidateBaltop()
        loadBalancesAsync {
            currentPage = 0
            refreshDynamicItems()
        }

        messageManager.sendMessageFromConfig(player, "gui.refresh-success")
    }

    private fun sendCooldownMessage(player: Player, refreshCooldown: Long, currentTime: Long) {
        val remaining = ((refreshCooldown - (currentTime - lastRefreshTime)) / 1000.0)
        val placeholders = Placeholders().add("remaining", String.format("%.1f", remaining))
        messageManager.sendMessageFromConfig(player, "gui.refresh-cooldown", placeholders)
        soundManager.playErrorSound(player)
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
                if (plugin.config.getBoolean("stats.enabled", true) &&
                    plugin.config.getBoolean("stats.open-clicked-player", true)) {
                    plugin.foliaLib.scheduler.runAtEntity(player) {
                        plugin.statsGui.openForPlayer(player, uuid, true, currentPage)
                    }
                }
            } else {
                messageManager.sendMessageFromConfig(player, "gui.player-data-not-found")
            }
        } catch (_: IllegalArgumentException) {
            plugin.logger.warning("Invalid UUID in baltop entry: $playerUuid")
        }
    }

    private fun getRefreshCooldown(): Long {
        return (config.getConfigurationSection("items.refresh")?.getLong("cooldown", 5) ?: 5) * 1000
    }

    private fun loadBalances() {
        balances = try {
            CacheManager.getBaltop()
        } catch (e: Exception) {
            plugin.logger.warning("Failed to load balances: ${e.message}")
            emptyList()
        }
    }

    private fun loadBalancesAsync(onComplete: () -> Unit = {}) {
        Threads.runAsync {
            try {
                val cachedBalances = CacheManager.getBaltop()
                Threads.runSync {
                    balances = cachedBalances
                    onComplete()
                }
            } catch (e: Exception) {
                plugin.logger.warning("Failed to load balances async: ${e.message}")
            }
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
            CacheManager.getSkinUrl(uuid)
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
        balances = CacheManager.getBaltop()

        if (CacheManager.isBaltopStale()) {
            CacheManager.refreshBaltopAsync()
        }

        currentPage = 0
        refreshDynamicItems()

        val playerItem = createPlayerItem(player)
        if (playerItem != null) {
            inventory.setItem(48, playerItem)
        }

        super.open(player)
    }

    override fun reload() {
        loadBalances()
        super.reload()
    }
}