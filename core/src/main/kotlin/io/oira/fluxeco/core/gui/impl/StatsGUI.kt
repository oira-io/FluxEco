package io.oira.fluxeco.core.gui.impl

import io.oira.fluxeco.core.FluxEco
import io.oira.fluxeco.core.cache.CacheManager
import io.oira.fluxeco.core.gui.BaseGUI
import io.oira.fluxeco.core.util.Placeholders
import io.oira.fluxeco.core.util.format
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

class StatsGUI : BaseGUI("gui/stats-ui.yml") {

    private var targetPlayerUuid: UUID? = null
    private var openedFromBaltop: Boolean = false
    private var baltopPage: Int = 0

    init {
        registerActions()
        initialize()
    }

    private fun registerActions() {
        registerSimpleAction("cancel") { }
        registerSimpleAction("close") { it.closeInventory() }
        registerSimpleAction("back") { player ->
            if (openedFromBaltop) {
                player.closeInventory()
                FluxEco.instance.baltopGui.apply {
                    currentPage = baltopPage
                    open(player)
                }
            }
        }
    }

    fun openForPlayer(viewer: Player, targetUuid: UUID, fromBaltop: Boolean = false, page: Int = 0) {
        targetPlayerUuid = targetUuid
        openedFromBaltop = fromBaltop
        baltopPage = page
        refreshItems()
        super.open(viewer)
    }

    override fun open(player: Player) = openForPlayer(player, player.uniqueId)

    override fun getTitlePlaceholders(): Placeholders {
        val uuid = targetPlayerUuid ?: return Placeholders()
        val target = Bukkit.getOfflinePlayer(uuid)
        val playerName = target.name ?: "Unknown"

        return Placeholders()
            .add("player", playerName)
    }

    private fun refreshItems() {
        val uuid = targetPlayerUuid ?: return
        val target = Bukkit.getOfflinePlayer(uuid)
        val player = target.player

        val itemsSection = config.getConfigurationSection("items") ?: return
        itemsSection.getKeys(false).forEach { key ->
            if (key == "back" && !openedFromBaltop) return@forEach

            val itemConfig = itemsSection.getConfigurationSection(key) ?: return@forEach
            val slot = itemConfig.getInt("slot", -1)
            if (slot >= 0 && slot < inventory.size) {
                val placeholders = createPlayerPlaceholders(player)
                val item = createItemFromConfig(itemConfig, key, placeholders)
                if (item != null) {
                    inventory.setItem(slot, item)
                }
            }
        }
    }

    override fun onPreInit() {
        super.onPreInit()
        refreshItems()
    }

    private fun createPlayerPlaceholders(player: Player?): Placeholders {
        val uuid = targetPlayerUuid ?: return Placeholders()
        val balance = CacheManager.getBalance(uuid)

        return Placeholders()
            .add("balance", balance.format())
            .setPlayer(player)
    }

    override fun reload() {
        if (targetPlayerUuid != null) {
            refreshItems()
        }
        super.reload()
    }
}

