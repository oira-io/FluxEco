package io.oira.fluxeco.core.data.mongodb.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Sorts
import io.oira.fluxeco.core.data.model.Transaction
import io.oira.fluxeco.core.data.model.TransactionType
import io.oira.fluxeco.core.data.mongodb.MongoDBManager
import io.oira.fluxeco.core.data.mongodb.document.TransactionDocument
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import java.util.UUID

object MongoTransactionRepository {
    private fun getCollection() = MongoDBManager.getTransactionsCollection()

    fun getTransactions(uuid: UUID): List<Transaction> = runBlocking {
        getCollection().find(Filters.eq("playerUuid", uuid.toString()))
            .sort(Sorts.descending("date"))
            .toList()
            .map { doc ->
                Transaction(
                    id = doc.id,
                    playerUuid = doc.getPlayerUUID(),
                    type = TransactionType.valueOf(doc.type),
                    amount = doc.amount,
                    senderUuid = doc.getSenderUUID(),
                    receiverUuid = doc.getReceiverUUID(),
                    date = doc.date
                )
            }
    }

    fun createTransaction(
        id: Int,
        playerUuid: UUID,
        type: TransactionType,
        amount: Double,
        senderUuid: UUID,
        receiverUuid: UUID
    ): Int = runBlocking {
        val doc = TransactionDocument.create(
            id = id,
            playerUuid = playerUuid,
            type = type.name,
            amount = amount,
            senderUuid = senderUuid,
            receiverUuid = receiverUuid,
            date = System.currentTimeMillis()
        )
        getCollection().insertOne(doc)
        id
    }

    fun deleteTransaction(id: Int): Int = runBlocking {
        val result = getCollection().deleteOne(Filters.eq("_id", id))
        result.deletedCount.toInt()
    }

    fun deleteAllTransactions(): Int = runBlocking {
        val result = getCollection().deleteMany(Filters.empty())
        result.deletedCount.toInt()
    }

    fun getMaxTransactionId(): Int = runBlocking {
        val doc = getCollection().find()
            .sort(Sorts.descending("_id"))
            .limit(1)
            .toList()
            .firstOrNull()
        doc?.id ?: 0
    }

    fun transactionExists(id: Int): Boolean = runBlocking {
        getCollection().find(Filters.eq("_id", id)).limit(1).toList().isNotEmpty()
    }
}

