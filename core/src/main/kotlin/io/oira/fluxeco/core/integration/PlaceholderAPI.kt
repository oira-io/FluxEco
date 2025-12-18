package io.oira.fluxeco.core.integration

import io.oira.fluxeco.core.cache.CacheManager
import io.oira.fluxeco.core.manager.SettingsManager
import io.oira.fluxeco.core.util.format
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.jetbrains.annotations.NotNull

class PlaceholderAPI : PlaceholderExpansion() {

    @NotNull
    override fun getAuthor(): String {
        return "Harfull"
    }

    @NotNull
    override fun getIdentifier(): String {
        return "FluxEco"
    }

    @NotNull
    override fun getVersion(): String {
        return "1.0.0"
    }

    override fun onRequest(player: OfflinePlayer?, @NotNull params: String): String? {
        if (player == null) return null
        return when (params.lowercase()) {
            "balance" -> CacheManager.getBalance(player.uniqueId).toString()
            "balance_formatted" -> CacheManager.getBalance(player.uniqueId).format()
            "toggle_payments" -> if (SettingsManager.getTogglePayments(player.uniqueId)) "enabled" else "disabled"
            "pay_alerts" -> if (SettingsManager.getPayAlerts(player.uniqueId)) "enabled" else "disabled"
            else -> null
        }
    }

    override fun onPlaceholderRequest(player: Player?, @NotNull params: String): String? {
        if (player == null) return null
        return when (params.lowercase()) {
            "balance" -> CacheManager.getBalance(player.uniqueId).toString()
            "balance_formatted" -> CacheManager.getBalance(player.uniqueId).format()
            "toggle_payments" -> if (SettingsManager.getTogglePayments(player.uniqueId)) "enabled" else "disabled"
            "pay_alerts" -> if (SettingsManager.getPayAlerts(player.uniqueId)) "enabled" else "disabled"
            else -> null
        }
    }
}
