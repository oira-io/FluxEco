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

import io.oira.fluxeco.FluxEco
import io.oira.fluxeco.gui.BaseGUI
import io.oira.fluxeco.manager.CacheManager
import io.oira.fluxeco.util.Placeholders
import io.oira.fluxeco.util.format
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
                plugin.foliaLib.scheduler.runAtEntity(player) {
                    FluxEco.instance.baltopGui.apply {
                        currentPage = baltopPage
                        open(player)
                    }                }
            }
        }
    }

    fun openForPlayer(viewer: Player, targetUuid: UUID, fromBaltop: Boolean = false, page: Int = 0) {
        targetPlayerUuid = targetUuid
        openedFromBaltop = fromBaltop
        baltopPage = page
        refreshItems()
        updateTitle()
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
            val itemConfig = itemsSection.getConfigurationSection(key) ?: return@forEach
            val slot = itemConfig.getInt("slot", -1)

            if (key == "back") {
                if (!openedFromBaltop) {
                    if (slot >= 0 && slot < inventory.size) {
                        inventory.setItem(slot, null)
                        usedSlots.remove(slot)
                    }
                    return@forEach
                } else {
                    if (slot >= 0 && slot < inventory.size) {
                        val placeholders = createPlayerPlaceholders(player)
                        val item = createItemFromConfig(itemConfig, key, placeholders)
                        if (item != null) {
                            inventory.setItem(slot, item)
                            usedSlots.add(slot)
                        }
                    }
                    return@forEach
                }
            }

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
        val itemsSection = config.getConfigurationSection("items") ?: return
        val backConfig = itemsSection.getConfigurationSection("back")
        if (backConfig != null) {
            val backSlot = backConfig.getInt("slot", -1)
            if (backSlot >= 0) {
                usedSlots.remove(backSlot)
                inventory.setItem(backSlot, null)
            }
        }
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

