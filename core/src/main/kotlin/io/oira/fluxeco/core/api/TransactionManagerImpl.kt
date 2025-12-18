package io.oira.fluxeco.core.api

import io.oira.fluxeco.api.model.Transaction
import io.oira.fluxeco.api.model.TransactionType
import io.oira.fluxeco.api.transaction.ITransactionManager
import io.oira.fluxeco.core.data.manager.TransactionDataManager
import io.oira.fluxeco.core.manager.TransactionManager
import java.util.*
import java.util.concurrent.CompletableFuture

class TransactionManagerImpl : ITransactionManager {

    override fun getTransactionHistory(uuid: UUID): List<Transaction> {
        return TransactionManager.getTransactionHistory(uuid).map { tx ->
            Transaction(
                id = tx.id,
                playerUuid = tx.playerUuid,
                type = TransactionType.valueOf(tx.type.name),
                amount = tx.amount,
                senderUuid = tx.senderUuid,
                receiverUuid = tx.receiverUuid,
                date = tx.date
            )
        }
    }

    override fun getTransactionHistoryAsync(uuid: UUID): CompletableFuture<List<Transaction>> {
        return CompletableFuture.supplyAsync {
            getTransactionHistory(uuid)
        }
    }

    override fun recordTransfer(from: UUID, to: UUID, amount: Double) {
        TransactionManager.recordTransfer(from, to, amount)
    }

    override fun recordAdminDeduct(player: UUID, amount: Double, adminUuid: UUID) {
        TransactionManager.recordAdminDeduct(player, amount, adminUuid)
    }

    override fun recordAdminReceive(player: UUID, amount: Double, adminUuid: UUID) {
        TransactionManager.recordAdminReceive(player, amount, adminUuid)
    }

    override fun getTransactionCount(uuid: UUID): Int {
        return TransactionDataManager.getTransactions(uuid).size
    }

    override fun clearTransactionHistory(uuid: UUID): Int {
        return TransactionDataManager.deleteTransactions(uuid)
    }
}
