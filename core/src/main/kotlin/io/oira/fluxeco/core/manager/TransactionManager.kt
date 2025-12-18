package io.oira.fluxeco.core.manager

import io.oira.fluxeco.core.cache.CacheManager
import io.oira.fluxeco.core.data.manager.TransactionDataManager
import io.oira.fluxeco.core.data.model.Transaction
import io.oira.fluxeco.core.data.model.TransactionType
import io.oira.fluxeco.core.util.Threads
import java.util.*

object TransactionManager {

    fun getTransactionHistory(uuid: UUID): List<Transaction> {
        return CacheManager.getTransactions(uuid)
    }

    fun recordTransfer(from: UUID, to: UUID, amount: Double) {
        Threads.runAsync {
            TransactionDataManager.createTransaction(from, TransactionType.SENT, amount, from, to)
            TransactionDataManager.createTransaction(to, TransactionType.RECEIVED, amount, from, to)
            CacheManager.invalidateTransactions(from)
            CacheManager.invalidateTransactions(to)
        }
    }

    fun recordAdminDeduct(player: UUID, amount: Double, adminUuid: UUID = UUID(0, 0)) {
        Threads.runAsync {
            TransactionDataManager.createTransaction(player, TransactionType.ADMIN_DEDUCTED, amount, adminUuid, player)
            CacheManager.invalidateTransactions(player)
        }
    }

    fun recordAdminReceive(player: UUID, amount: Double, adminUuid: UUID = UUID(0, 0)) {
        Threads.runAsync {
            TransactionDataManager.createTransaction(player, TransactionType.ADMIN_RECEIVED, amount, adminUuid, player)
            CacheManager.invalidateTransactions(player)
        }
    }
}
