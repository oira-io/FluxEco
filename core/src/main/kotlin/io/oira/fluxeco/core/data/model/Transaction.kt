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

package io.oira.fluxeco.core.data.model

import java.util.*

enum class TransactionType {
    SENT,
    RECEIVED,
    ADMIN_DEDUCTED,
    ADMIN_RECEIVED
}

data class Transaction(
    val id: Int,
    val playerUuid: UUID,
    val type: TransactionType,
    val amount: Double,
    val senderUuid: UUID,
    val receiverUuid: UUID,
    val date: Long
)
