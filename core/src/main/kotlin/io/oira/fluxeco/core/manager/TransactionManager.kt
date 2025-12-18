package io.oira.fluxeco.core.manager

import io.oira.fluxeco.core.cache.CacheManager
import io.oira.fluxeco.core.data.manager.TransactionDataManager
import io.oira.fluxeco.core.data.model.Transaction
import io.oira.fluxeco.core.data.model.TransactionType
import io.oira.fluxeco.core.util.Threads
import java.util.*

object TransactionManager {

    /**
     * Retrieve the transaction history for the given UUID.
     *
     * @param uuid The UUID whose transactions are requested.
     * @return A list of Transaction objects for the specified UUID; an empty list if there are no transactions.
     */
    fun getTransactionHistory(uuid: UUID): List<Transaction> {
        return CacheManager.getTransactions(uuid)
    }

    /**
     * Records a transfer between two accounts by creating a SENT transaction for the sender and a RECEIVED transaction for the receiver, then invalidates both accounts' transaction caches.
     *
     * @param from UUID of the sender.
     * @param to UUID of the receiver.
     * @param amount Transfer amount. 
     */
    fun recordTransfer(from: UUID, to: UUID, amount: Double) {
        Threads.runAsync {
            TransactionDataManager.createTransaction(from, TransactionType.SENT, amount, from, to)
            TransactionDataManager.createTransaction(to, TransactionType.RECEIVED, amount, from, to)
            CacheManager.invalidateTransactions(from)
            CacheManager.invalidateTransactions(to)
        }
    }

    /**
     * Records an administrative deduction transaction for a player and invalidates that player's cached transactions.
     *
     * Creates a Transaction of type `ADMIN_DEDUCTED` with the given amount and administrator UUID, then clears the player's transaction cache so subsequent reads reflect the change.
     *
     * @param player The UUID of the player whose balance is being deducted.
     * @param amount The amount to deduct from the player's balance.
     * @param adminUuid The UUID of the administrator performing the deduction (defaults to `UUID(0, 0)` when unspecified).
     */
    fun recordAdminDeduct(player: UUID, amount: Double, adminUuid: UUID = UUID(0, 0)) {
        Threads.runAsync {
            TransactionDataManager.createTransaction(player, TransactionType.ADMIN_DEDUCTED, amount, adminUuid, player)
            CacheManager.invalidateTransactions(player)
        }
    }

    /**
     * Record an administrative credit to a player's transaction history.
     *
     * Creates an ADMIN_RECEIVED transaction with the given amount and invalidates the player's cached transactions.
     *
     * @param player UUID of the player receiving the credit.
     * @param amount Amount credited to the player's account.
     * @param adminUuid UUID of the administrator who issued the credit; defaults to UUID(0, 0).
     */
    fun recordAdminReceive(player: UUID, amount: Double, adminUuid: UUID = UUID(0, 0)) {
        Threads.runAsync {
            TransactionDataManager.createTransaction(player, TransactionType.ADMIN_RECEIVED, amount, adminUuid, player)
            CacheManager.invalidateTransactions(player)
        }
    }
}