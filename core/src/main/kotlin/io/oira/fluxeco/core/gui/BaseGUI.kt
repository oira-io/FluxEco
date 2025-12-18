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

package io.oira.fluxeco.core.gui

import io.oira.fluxeco.core.FluxEco
import io.oira.fluxeco.core.manager.ConfigManager
import io.oira.fluxeco.core.manager.MessageManager
import io.oira.fluxeco.core.manager.SoundManager
import io.oira.fluxeco.core.util.Placeholders
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

abstract class BaseGUI(private val configFile: String) : InventoryHolder, Listener {

    protected val plugin: FluxEco = FluxEco.instance
    protected val messageManager: MessageManager = MessageManager.getInstance()
    protected val soundManager: SoundManager = SoundManager.getInstance()
    protected val configManager: ConfigManager = ConfigManager(plugin, configFile)
    protected val config = configManager.getConfig()
    protected val actionKey = NamespacedKey(plugin, "action")
    protected val guiIdKey = NamespacedKey(plugin, "gui_id")
    protected val soundKey = NamespacedKey(plugin, "sound")
    protected val guiId: String = "${System.currentTimeMillis()}_${hashCode()}"
    protected val viewers = mutableSetOf<Player>()
    protected val actions = mutableMapOf<String, (Player, InventoryClickEvent) -> Unit>()
    protected val usedSlots = mutableSetOf<Int>()
    protected var currentPage = 0
    protected var entriesPerPage = 45
    protected var currentSort: String = "default"
    protected var sortOptions: List<String> = emptyList()
    private var internalSearchQuery: String = ""

    protected val searchQuery: String
        get() = internalSearchQuery

    private var privateInventory: Inventory
    private var isInitialized = false

    init {
        privateInventory = createInventory()
    }

    fun initialize() {
        try {
            onPreInit()
            loadStaticItems()
            onPostInit()
            updateInventory()
            isInitialized = true
        } catch (e: Exception) {
            plugin.logger.severe("Failed to initialize GUI $configFile: ${e.message}")
            e.printStackTrace()
        }
    }

    protected open fun onPreInit() {}
    protected open fun onPostInit() {}

    private fun createInventory(): Inventory {
        val size = config.getInt("gui-size", 54).coerceIn(9, 54)
        val validSize = (size / 9) * 9
        val title = config.getString("title", "GUI") ?: "GUI"
        return Bukkit.createInventory(this, validSize, messageManager.processColors(title, Placeholders()))
    }

    private fun updateInventory() {
        val title = getTitleComponent()
        val size = privateInventory.size
        val newInventory = Bukkit.createInventory(this, size, title)
        for (i in 0 until size) {
            newInventory.setItem(i, privateInventory.getItem(i))
        }
        privateInventory = newInventory
    }

    protected open fun getTitleComponent(): Component {
        val titleString = if (internalSearchQuery.isEmpty()) {
            config.getString("title", "GUI") ?: "GUI"
        } else {
            config.getString("search-title", config.getString("title", "GUI") ?: "GUI") ?: "GUI"
        }
        val placeholders = getTitlePlaceholders()
        if (internalSearchQuery.isNotEmpty()) {
            placeholders.add("query", internalSearchQuery)
        }
        return messageManager.processColors(titleString, placeholders)
    }

    protected open fun getTitlePlaceholders(): Placeholders {
        return Placeholders()
            .add("page", (currentPage + 1).toString())
            .add("max-page", getMaxPages().toString())
    }

    private fun loadStaticItems() {
        val itemsSection = config.getConfigurationSection("items") ?: return
        itemsSection.getKeys(false)
            .filter { it != "entry" }
            .mapNotNull { key -> itemsSection.getConfigurationSection(key)?.let { key to it } }
            .forEach { (key, itemConfig) ->
                val slot = itemConfig.getInt("slot", -1)
                if (isValidSlot(slot)) {
                    createItemFromConfig(itemConfig, key)?.let { item ->
                        privateInventory.setItem(slot, item)
                        usedSlots.add(slot)
                    }
                }
            }
    }

    protected fun createItemFromConfig(itemConfig: ConfigurationSection, key: String, placeholders: Placeholders = Placeholders()): ItemStack? {
        val materialName = itemConfig.getString("material", "STONE") ?: "STONE"
        val material = try { Material.valueOf(materialName.uppercase()) } catch (_: IllegalArgumentException) { Material.STONE }
        val item = ItemStack(material, itemConfig.getInt("amount", 1).coerceIn(1, 64))
        val meta = item.itemMeta ?: return null
        meta.displayName(messageManager.processColors(itemConfig.getString("name", "") ?: "", placeholders))
        meta.lore(itemConfig.getStringList("lore").map { messageManager.processColors(it, placeholders) })
        if (itemConfig.getInt("custom-model-data", -1) > 0) meta.setCustomModelData(itemConfig.getInt("custom-model-data"))
        val container = meta.persistentDataContainer
        container.set(actionKey, PersistentDataType.STRING, itemConfig.getString("action", "none") ?: "none")
        container.set(guiIdKey, PersistentDataType.STRING, guiId)
        container.set(soundKey, PersistentDataType.STRING, itemConfig.getString("sound", "none") ?: "none")
        item.itemMeta = meta
        return item
    }

    protected open fun getEntryData(): List<Any> = emptyList()

    protected open fun createEntryItem(data: Any, index: Int): ItemStack? = null

    protected open fun filterData(data: List<Any>): List<Any> = data

    protected fun loadDynamicItems() {
        if (!isInitialized) return
        val data = getSortedData()
        if (data.isEmpty()) return
        val startIndex = currentPage * entriesPerPage
        val endIndex = minOf(startIndex + entriesPerPage, data.size)
        if (startIndex >= data.size) return
        val pageData = data.subList(startIndex, endIndex)
        pageData.forEachIndexed { index, item ->
            if (index < inventory.size && !usedSlots.contains(index)) {
                createEntryItem(item, index)?.let { privateInventory.setItem(index, it) }
            }
        }
    }

    protected open fun getMaxPages(): Int {
        val data = getSortedData()
        return if (data.isEmpty()) 1 else ((data.size - 1) / entriesPerPage) + 1
    }

    protected fun registerAction(action: String, handler: (Player, InventoryClickEvent) -> Unit) {
        actions[action] = handler
    }

    protected fun registerSimpleAction(action: String, handler: (Player) -> Unit) {
        actions[action] = { player, _ -> handler(player) }
    }

    protected fun refreshDynamicItems() {
        if (!isInitialized) return
        clearDynamicSlots()
        loadDynamicItems()
        updateTitle()
        viewers.forEach { it.updateInventory() }
    }

    private fun updateTitle() {
        val title = getTitleComponent()
        val size = privateInventory.size
        val newInventory = Bukkit.createInventory(this, size, title)
        for (i in 0 until size) newInventory.setItem(i, privateInventory.getItem(i))
        privateInventory = newInventory
        viewers.forEach { viewer -> if (viewer.openInventory.topInventory.holder == this) viewer.openInventory(privateInventory) }
    }

    private fun clearDynamicSlots() {
        for (slot in 0 until privateInventory.size) if (!usedSlots.contains(slot)) privateInventory.setItem(slot, null)
    }

    private fun isValidSlot(slot: Int) = slot >= 0 && slot < privateInventory.size && !usedSlots.contains(slot)

    open fun open(player: Player) {
        if (!isInitialized) return
        viewers.add(player)
        loadDynamicItems()
        player.openInventory(privateInventory)
        soundManager.playSoundFromConfig(player, "open", configManager)
        onOpen(player)
    }

    protected open fun onOpen(player: Player) {}

    fun close(player: Player) {
        player.closeInventory()
        viewers.remove(player)
        onClose(player)
    }

    protected open fun onClose(player: Player) {
        internalSearchQuery = ""
    }

    fun closeAll() {
        viewers.toList().forEach { it.closeInventory(); onClose(it) }
        viewers.clear()
    }

    override fun getInventory(): Inventory = privateInventory

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        if (event.view.topInventory.holder != this) return
        event.isCancelled = true
        val item = event.currentItem ?: return
        val meta = item.itemMeta ?: return
        val container = meta.persistentDataContainer
        val guiIdValue = container.get(guiIdKey, PersistentDataType.STRING) ?: return
        if (guiIdValue != guiId) return
        val action = container.get(actionKey, PersistentDataType.STRING) ?: "none"
        val sound = container.get(soundKey, PersistentDataType.STRING) ?: "none"
        actions[action]?.invoke(player, event)
        if (sound != "none") soundManager.playSoundFromConfig(player, sound, configManager)
    }

    fun setSortOptions(options: List<String>, defaultSort: String = "default") {
        sortOptions = options
        currentSort = defaultSort
    }

    fun cycleSort() {
        if (sortOptions.isEmpty()) return
        val currentIndex = sortOptions.indexOf(currentSort)
        currentSort = if (currentIndex == -1) sortOptions[0] else sortOptions[(currentIndex + 1) % sortOptions.size]
        currentPage = 0
        refreshDynamicItems()
    }

    protected open fun getSortedData(): List<Any> {
        val data = getEntryData()
        val filtered = filterData(data)
        if (sortOptions.isEmpty()) return filtered
        return when (currentSort) {
            "default" -> filtered
            else -> filtered
        }
    }

    fun setSearchQuery(query: String) {
        internalSearchQuery = query
        currentPage = 0
        refreshDynamicItems()
    }

    fun clearSearch() {
        internalSearchQuery = ""
        currentPage = 0
        refreshDynamicItems()
    }

    open fun reload() {
        configManager.reloadConfig()
        initialize()
    }
}