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
