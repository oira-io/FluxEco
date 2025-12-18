package io.oira.fluxeco.core.integration

import io.oira.fluxeco.core.FluxEco
import io.oira.fluxeco.core.cache.CacheManager
import io.oira.fluxeco.core.manager.EconomyManager
import io.oira.fluxeco.core.util.format
import net.milkbowl.vault.economy.AbstractEconomy
import net.milkbowl.vault.economy.EconomyResponse
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer

class Vault(private val plugin: FluxEco, private val economyManager: EconomyManager) : AbstractEconomy() {

    private val currencyNameSingular: String = EconomyManager.getCurrencyName(1.0)
    private val currencyNamePlural: String = EconomyManager.getCurrencyName(2.0)

    override fun isEnabled(): Boolean {
        return plugin.isEnabled
    }

    override fun getName(): String {
        return plugin.name
    }

    override fun hasBankSupport(): Boolean {
        return false
    }

    override fun fractionalDigits(): Int {
        return 2
    }

    override fun format(amount: Double): String {
        return amount.format()
    }

    override fun currencyNamePlural(): String {
        return currencyNamePlural
    }

    override fun currencyNameSingular(): String {
        return currencyNameSingular
    }

    override fun hasAccount(player: OfflinePlayer): Boolean {
        return true
    }

    /**
     * Checks whether the specified player has an account; the provided world name is ignored.
     *
     * @param player The player to check.
     * @param worldName Ignored; kept for API compatibility.
     * @return `true` if the player has an account, `false` otherwise.
     */
    override fun hasAccount(player: OfflinePlayer, worldName: String): Boolean {
        return hasAccount(player)
    }

    /**
     * Retrieve the cached balance for the given player.
     *
     * @param player The player whose cached balance will be returned.
     * @return The player's current balance as stored in the cache.
     */
    override fun getBalance(player: OfflinePlayer): Double {
        return CacheManager.getBalance(player.uniqueId)
    }

    /**
     * Retrieve the specified player's balance; the `world` parameter is ignored.
     *
     * @param player The player whose balance to retrieve.
     * @param world The world name (not used by this implementation).
     * @return The player's current balance.
     */
    override fun getBalance(player: OfflinePlayer, world: String): Double {
        return getBalance(player)
    }

    /**
     * Check whether the player's cached balance is at least the given amount.
     *
     * @param player The player whose balance is checked.
     * @param amount The amount to compare against the player's balance.
     * @return `true` if the player's cached balance is greater than or equal to `amount`, `false` otherwise.
     */
    override fun has(player: OfflinePlayer, amount: Double): Boolean {
        return CacheManager.getBalance(player.uniqueId) >= amount
    }

    /**
     * Checks whether the player has at least the specified amount in their account.
     *
     * The `worldName` parameter is ignored; account checks are global.
     *
     * @param player The player whose balance to check.
     * @param worldName Ignored world name parameter kept for API compatibility.
     * @param amount The amount to verify against the player's balance.
     * @return `true` if the player's balance is greater than or equal to `amount`, `false` otherwise.
     */
    override fun has(player: OfflinePlayer, worldName: String, amount: Double): Boolean {
        return has(player, amount)
    }

    /**
     * Attempts to withdraw a specified amount from the player's account.
     *
     * @param player The player whose account to withdraw from.
     * @param amount The amount to withdraw.
     * @return An `EconomyResponse` with `ResponseType.SUCCESS` and the updated balance when the withdrawal succeeds;
     * `ResponseType.FAILURE` with the message "Not enough money" and the current balance when it fails.
     */
    override fun withdrawPlayer(player: OfflinePlayer, amount: Double): EconomyResponse {
        val currentBalance = CacheManager.getBalance(player.uniqueId)
        if (currentBalance >= amount) {
            economyManager.subtractBalance(player.uniqueId, amount)
            val newBalance = CacheManager.getBalance(player.uniqueId)
            return EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, null)
        }
        return EconomyResponse(0.0, currentBalance, EconomyResponse.ResponseType.FAILURE, "Not enough money")
    }

    override fun withdrawPlayer(player: OfflinePlayer, worldName: String, amount: Double): EconomyResponse {
        return withdrawPlayer(player, amount)
    }

    /**
     * Deposits the specified amount into the player's account and returns the resulting economy response.
     *
     * @param player The target offline player whose account will be credited.
     * @param amount The amount to add to the player's balance.
     * @return An EconomyResponse with `ResponseType.SUCCESS`, the deposited `amount`, and the player's updated balance.
     */
    override fun depositPlayer(player: OfflinePlayer, amount: Double): EconomyResponse {
        economyManager.addBalance(player.uniqueId, amount)
        val newBalance = CacheManager.getBalance(player.uniqueId)
        return EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, null)
    }

    override fun depositPlayer(player: OfflinePlayer, worldName: String, amount: Double): EconomyResponse {
        return depositPlayer(player, amount)
    }

    override fun createPlayerAccount(player: OfflinePlayer): Boolean {
        economyManager.createBalance(player.uniqueId)
        return true
    }

    override fun createPlayerAccount(player: OfflinePlayer, worldName: String): Boolean {
        return createPlayerAccount(player)
    }

    override fun createBank(name: String, player: OfflinePlayer): EconomyResponse {
        return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported")
    }

    override fun isBankOwner(name: String, player: OfflinePlayer): EconomyResponse {
        return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported")
    }

    override fun isBankMember(name: String, player: OfflinePlayer): EconomyResponse {
        return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported")
    }

    @Deprecated("Use hasAccount(OfflinePlayer)")
    override fun hasAccount(playerName: String): Boolean {
        return hasAccount(Bukkit.getOfflinePlayer(playerName))
    }

    @Deprecated("Use hasAccount(OfflinePlayer, String)")
    override fun hasAccount(playerName: String, worldName: String): Boolean {
        return hasAccount(Bukkit.getOfflinePlayer(playerName), worldName)
    }

    @Deprecated("Use getBalance(OfflinePlayer)")
    override fun getBalance(playerName: String): Double {
        return getBalance(Bukkit.getOfflinePlayer(playerName))
    }

    @Deprecated("Use getBalance(OfflinePlayer, String)")
    override fun getBalance(playerName: String, world: String): Double {
        return getBalance(Bukkit.getOfflinePlayer(playerName), world)
    }

    @Deprecated("Use has(OfflinePlayer, double)")
    override fun has(playerName: String, amount: Double): Boolean {
        return has(Bukkit.getOfflinePlayer(playerName), amount)
    }

    @Deprecated("Use has(OfflinePlayer, String, double)")
    override fun has(playerName: String, worldName: String, amount: Double): Boolean {
        return has(Bukkit.getOfflinePlayer(playerName), worldName, amount)
    }

    @Deprecated("Use withdrawPlayer(OfflinePlayer, double)")
    override fun withdrawPlayer(playerName: String, amount: Double): EconomyResponse {
        return withdrawPlayer(Bukkit.getOfflinePlayer(playerName), amount)
    }

    @Deprecated("Use withdrawPlayer(OfflinePlayer, String, double)")
    override fun withdrawPlayer(playerName: String, worldName: String, amount: Double): EconomyResponse {
        return withdrawPlayer(Bukkit.getOfflinePlayer(playerName), worldName, amount)
    }

    @Deprecated("Use depositPlayer(OfflinePlayer, double)")
    override fun depositPlayer(playerName: String, amount: Double): EconomyResponse {
        return depositPlayer(Bukkit.getOfflinePlayer(playerName), amount)
    }

    @Deprecated("Use depositPlayer(OfflinePlayer, String, double)")
    override fun depositPlayer(playerName: String, worldName: String, amount: Double): EconomyResponse {
        return depositPlayer(Bukkit.getOfflinePlayer(playerName), worldName, amount)
    }

    @Deprecated("Use createBank(String, OfflinePlayer)")
    override fun createBank(name: String, playerName: String): EconomyResponse {
        return createBank(name, Bukkit.getOfflinePlayer(playerName))
    }

    @Deprecated("Use isBankOwner(String, OfflinePlayer)")
    override fun isBankOwner(name: String, playerName: String): EconomyResponse {
        return isBankOwner(name, Bukkit.getOfflinePlayer(playerName))
    }

    @Deprecated("Use isBankMember(String, OfflinePlayer)")
    override fun isBankMember(name: String, playerName: String): EconomyResponse {
        return isBankMember(name, Bukkit.getOfflinePlayer(playerName))
    }

    @Deprecated("Use createPlayerAccount(OfflinePlayer)")
    override fun createPlayerAccount(playerName: String): Boolean {
        return createPlayerAccount(Bukkit.getOfflinePlayer(playerName))
    }

    @Deprecated("Use createPlayerAccount(OfflinePlayer, String)")
    override fun createPlayerAccount(playerName: String, worldName: String): Boolean {
        return createPlayerAccount(Bukkit.getOfflinePlayer(playerName), worldName)
    }

    override fun deleteBank(name: String): EconomyResponse {
        return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported")
    }

    override fun bankBalance(name: String): EconomyResponse {
        return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported")
    }

    override fun bankHas(name: String, amount: Double): EconomyResponse {
        return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported")
    }

    override fun bankWithdraw(name: String, amount: Double): EconomyResponse {
        return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported")
    }

    override fun bankDeposit(name: String, amount: Double): EconomyResponse {
        return EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported")
    }

    override fun getBanks(): List<String> {
        return emptyList()
    }
}