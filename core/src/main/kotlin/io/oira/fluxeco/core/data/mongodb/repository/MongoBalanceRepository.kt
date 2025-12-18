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

package io.oira.fluxeco.core.data.mongodb.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import io.oira.fluxeco.core.data.model.Balance
import io.oira.fluxeco.core.data.mongodb.MongoDBManager
import io.oira.fluxeco.core.data.mongodb.document.BalanceDocument
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import java.util.*

object MongoBalanceRepository {
    private fun getCollection() = MongoDBManager.getBalancesCollection()

    fun getAllBalances(): List<Balance> = runBlocking {
        getCollection().find().toList().map { doc ->
            Balance(
                uuid = doc.getUUID(),
                balance = doc.balance
            )
        }
    }

    fun getBalance(uuid: UUID): Balance? = runBlocking {
        val doc = getCollection().find(Filters.eq("_id", uuid.toString())).firstOrNull()
        doc?.let {
            Balance(
                uuid = it.getUUID(),
                balance = it.balance
            )
        }
    }

    fun createBalance(uuid: UUID, balance: Double) = runBlocking {
        val doc = BalanceDocument.fromUUID(uuid, balance)
        getCollection().insertOne(doc)
    }

    fun updateBalance(uuid: UUID, balance: Double): Int = runBlocking {
        val doc = BalanceDocument.fromUUID(uuid, balance)
        val result = getCollection().replaceOne(
            Filters.eq("_id", uuid.toString()),
            doc,
            ReplaceOptions().upsert(true)
        )
        if (result.modifiedCount > 0 || result.upsertedId != null) 1 else 0
    }

    fun deleteBalance(uuid: UUID): Int = runBlocking {
        val result = getCollection().deleteOne(Filters.eq("_id", uuid.toString()))
        result.deletedCount.toInt()
    }

    fun deleteAllBalances(): Int = runBlocking {
        val result = getCollection().deleteMany(Filters.empty())
        result.deletedCount.toInt()
    }
}

