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

package io.oira.fluxeco.api.transaction

import io.oira.fluxeco.api.model.Transaction
import java.util.*
import java.util.concurrent.CompletableFuture

interface ITransactionManager {

    fun getTransactionHistory(uuid: UUID): List<Transaction>

    fun getTransactionHistoryAsync(uuid: UUID): CompletableFuture<List<Transaction>>

    fun recordTransfer(from: UUID, to: UUID, amount: Double)

    fun recordAdminDeduct(player: UUID, amount: Double, adminUuid: UUID = UUID(0, 0))

    fun recordAdminReceive(player: UUID, amount: Double, adminUuid: UUID = UUID(0, 0))

    fun getTransactionCount(uuid: UUID): Int

    fun clearTransactionHistory(uuid: UUID): Int
}

