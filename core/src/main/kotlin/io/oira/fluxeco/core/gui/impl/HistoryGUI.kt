package io.oira.fluxeco.core.gui.impl

import io.oira.fluxeco.core.cache.CacheManager
import io.oira.fluxeco.core.data.model.Transaction
import io.oira.fluxeco.core.data.model.TransactionType
import io.oira.fluxeco.core.gui.BaseGUI
import io.oira.fluxeco.core.util.DateFormatter
import io.oira.fluxeco.core.util.Placeholders
import io.oira.fluxeco.core.util.format
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import java.util.*
import java.util.logging.Level

class HistoryGUI : BaseGUI("gui/history-ui.yml") {

    private val legacySerializer = LegacyComponentSerializer.legacyAmpersand()
    private var transactions: List<Transaction> = emptyList()
    private var targetPlayerUuid: UUID? = null

    init {
        setSortOptions(listOf("all", "sent", "received", "admin_received", "admin_deducted"), "all")
        registerActions()
        initialize()
    }

    private fun registerActions() {
        registerSimpleAction("previous_page") { handlePreviousPage(it) }
        registerSimpleAction("next_page") { handleNextPage(it) }
        registerSimpleAction("refresh") { handleRefresh(it) }
        registerSimpleAction("close") { it.closeInventory() }
        registerSimpleAction("sort") { cycleSort(); updateSortItem() }
        registerAction("entry") { _, _ -> }
    }

    private fun handlePreviousPage(player: Player) {
        if (currentPage > 0) currentPage-- else {
            messageManager.sendMessageFromConfig(player, "messages.previous-page-error", configManager)
            soundManager.playSoundFromConfig(player, "error", configManager)
        }
        refreshDynamicItems()
    }

    private fun handleNextPage(player: Player) {
        if (currentPage < getMaxPages() - 1) currentPage++ else {
            messageManager.sendMessageFromConfig(player, "messages.next-page-error", configManager)
            soundManager.playSoundFromConfig(player, "error", configManager)
        }
        refreshDynamicItems()
    }

    private fun handleRefresh(player: Player) {
        targetPlayerUuid?.let { CacheManager.invalidateTransactions(it) }

        loadTransactionsAsync {
            currentPage = 0
            refreshDynamicItems()
        }
        messageManager.sendMessageFromConfig(player, "messages.refresh-success", configManager)
    }

    private fun loadTransactions() {
        val uuid = targetPlayerUuid ?: return
        transactions = try {
            CacheManager.getTransactions(uuid)
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Failed to load transactions for $uuid: ${e.message}", e)
            emptyList()
        }
    }

    private fun loadTransactionsAsync(onComplete: () -> Unit = {}) {
        val uuid = targetPlayerUuid ?: return
        CacheManager.getTransactionsAsync(uuid) { txns ->
            transactions = txns
            onComplete()
        }
    }

    private fun getFilteredTransactions(): List<Transaction> {
        return when (currentSort) {
            "sent" -> transactions.filter { it.type == TransactionType.SENT }
            "received" -> transactions.filter { it.type == TransactionType.RECEIVED }
            "admin_received" -> transactions.filter { it.type == TransactionType.ADMIN_RECEIVED }
            "admin_deducted" -> transactions.filter { it.type == TransactionType.ADMIN_DEDUCTED }
            else -> transactions
        }
    }

    override fun getSortedData(): List<Any> = getFilteredTransactions()

    private fun createTransactionEntry(transaction: Transaction, entryConfig: ConfigurationSection): ItemStack {
        val viewerUuid = viewers.firstOrNull()?.uniqueId ?: return ItemStack(Material.AIR)
        val isGained = transaction.type == TransactionType.RECEIVED || transaction.type == TransactionType.ADMIN_RECEIVED
        val placeholders = createTransactionPlaceholders(transaction, viewerUuid, isGained, entryConfig)
        val material = Material.matchMaterial(entryConfig.getString("material", "PAPER")!!) ?: Material.PAPER
        val item = ItemStack(material)
        val meta = item.itemMeta ?: return item
        applyTransactionItemMeta(meta, placeholders, entryConfig, isGained)
        item.itemMeta = meta
        return item
    }

    private fun createTransactionPlaceholders(transaction: Transaction, viewerUuid: UUID, isGained: Boolean, entryConfig: ConfigurationSection): Placeholders {
        val amount = transaction.amount.format()
        val typeDisplay = getTypeDisplay(transaction.type, entryConfig)
        val fromName = getDisplayName(transaction.senderUuid, viewerUuid)
        val toName = getDisplayName(transaction.receiverUuid, viewerUuid)
        val date = DateFormatter.format(transaction.date)
        val amountKey = if (isGained) "gained-amount" else "lost-amount"
        val amountTemplate = entryConfig.getString(amountKey, "{amount}") ?: "{amount}"
        val formattedAmount = legacySerializer.serialize(messageManager.processColors(amountTemplate, Placeholders().add("amount", amount)))
        return Placeholders()
            .add("type", typeDisplay)
            .add("from", fromName)
            .add("to", toName)
            .add("amount", amount)
            .add("formatted-amount", formattedAmount)
            .add("date", date)
            .add("id", transaction.id.toString())
    }

    private fun getTypeDisplay(type: TransactionType, entryConfig: ConfigurationSection): String {
        return when (type) {
            TransactionType.SENT -> entryConfig.getString("types.sent", "Sent") ?: "Sent"
            TransactionType.RECEIVED -> entryConfig.getString("types.received", "Received") ?: "Received"
            TransactionType.ADMIN_RECEIVED -> entryConfig.getString("types.admin_received", "Admin Received") ?: "Admin Received"
            TransactionType.ADMIN_DEDUCTED -> entryConfig.getString("types.admin_deducted", "Admin Deducted") ?: "Admin Deducted"
        }
    }

    private fun applyTransactionItemMeta(meta: ItemMeta, placeholders: Placeholders, entryConfig: ConfigurationSection, isGained: Boolean) {
        val nameKey = if (isGained) "gained-name" else "lost-name"
        meta.displayName(messageManager.processColors(entryConfig.getString(nameKey, "{amount}") ?: "{amount}", placeholders))
        meta.lore(entryConfig.getStringList("lore").map { messageManager.processColors(it, placeholders) })
        val container = meta.persistentDataContainer
        container.set(actionKey, PersistentDataType.STRING, "entry")
        container.set(guiIdKey, PersistentDataType.STRING, guiId)
        container.set(soundKey, PersistentDataType.STRING, entryConfig.getString("sound", "none") ?: "none")
    }

    private fun getDisplayName(uuid: UUID, viewerUuid: UUID): String {
        if (uuid == UUID(0, 0)) return "Console"
        return if (uuid == viewerUuid) "You" else CacheManager.getPlayerProfile(uuid)?.name ?: Bukkit.getOfflinePlayer(uuid).name ?: "Unknown"
    }

    fun openForPlayer(viewer: Player, targetUuid: UUID) {
        targetPlayerUuid = targetUuid
        loadTransactions()
        currentPage = 0
        refreshDynamicItems()
        super.open(viewer)
        updateSortItem()
    }

    override fun open(player: Player) = openForPlayer(player, player.uniqueId)

    private fun updateSortItem() {
        val sortConfig = config.getConfigurationSection("items.sort") ?: return
        val slot = sortConfig.getInt("slot", 50)
        if (slot < 0 || slot >= inventory.size) return
        val item = inventory.getItem(slot) ?: ItemStack(Material.matchMaterial(sortConfig.getString("material", "COMPASS")!!) ?: Material.COMPASS)
        val meta = item.itemMeta ?: return
        val currentColor = sortConfig.getString("colors.current", "&#E53935") ?: "&#E53935"
        val otherColor = sortConfig.getString("colors.other", "&f") ?: "&f"
        val loreMap = sortConfig.getConfigurationSection("sort-lore") ?: return
        val lore = sortOptions.map { option ->
            val color = if (option == currentSort) currentColor else otherColor
            loreMap.getString(option, "∙ $option")?.replace("{color}", color) ?: "∙ $option"
        }.map { messageManager.processColors(it, Placeholders()) }
        meta.lore(lore)
        meta.displayName(messageManager.processColors(sortConfig.getString("name") ?: "&eSort", Placeholders()))
        item.itemMeta = meta
        inventory.setItem(slot, item)
    }

    override fun getEntryData(): List<Any> = getFilteredTransactions()

    override fun createEntryItem(data: Any, index: Int): ItemStack? {
        if (data !is Transaction) return null
        val entryConfig = config.getConfigurationSection("items.entry") ?: return null
        return createTransactionEntry(data, entryConfig)
    }

    override fun reload() {
        if (targetPlayerUuid != null) loadTransactions()
        super.reload()
        updateSortItem()
    }
}
