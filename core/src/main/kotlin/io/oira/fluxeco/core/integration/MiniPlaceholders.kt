package io.oira.fluxeco.core.integration

import io.github.miniplaceholders.kotlin.asInsertingTag
import io.github.miniplaceholders.kotlin.audience
import io.github.miniplaceholders.kotlin.expansion
import io.github.miniplaceholders.kotlin.global
import io.oira.fluxeco.core.cache.CacheManager
import io.oira.fluxeco.core.manager.SettingsManager
import io.oira.fluxeco.core.util.format
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class MiniPlaceholders {

    /**
     * Registers the "fluxeco" MiniPlaceholders expansion and its placeholders with the MiniPlaceholders system.
     *
     * The expansion provides:
     * - "balance": the player's raw balance as text.
     * - "balance_formatted": the player's formatted balance as text.
     * - "toggle_payments": "enabled" or "disabled" based on the player's payments toggle.
     * - "pay_alerts": "enabled" or "disabled" based on the player's pay alerts setting.
     * - "tps": the server's current tick-per-second value.
     */
    fun register() {
        val expansion = expansion("fluxeco") {
            audience<Player>("balance") { player, _, _ ->
                Component.text(CacheManager.getBalance(player.uniqueId).toString()).asInsertingTag()
            }
            audience<Player>("balance_formatted") { player, _, _ ->
                Component.text(CacheManager.getBalance(player.uniqueId).format()).asInsertingTag()
            }
            audience<Player>("toggle_payments") { player, _, _ ->
                Component.text(if (SettingsManager.getTogglePayments(player.uniqueId)) "enabled" else "disabled").asInsertingTag()
            }
            audience<Player>("pay_alerts") { player, _, _ ->
                Component.text(if (SettingsManager.getPayAlerts(player.uniqueId)) "enabled" else "disabled").asInsertingTag()
            }
            global("tps") { _, _ ->
                Component.text(Bukkit.getTPS()[0]).asInsertingTag()
            }
        }

        expansion.register()
    }

}