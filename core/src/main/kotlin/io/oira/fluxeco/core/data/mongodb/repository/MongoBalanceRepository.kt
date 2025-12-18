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
    /**
 * Retrieve the MongoDB collection used to store balance documents.
 *
 * @return The balances collection from the MongoDB manager.
 */
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
