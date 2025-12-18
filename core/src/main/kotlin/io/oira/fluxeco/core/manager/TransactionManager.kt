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
