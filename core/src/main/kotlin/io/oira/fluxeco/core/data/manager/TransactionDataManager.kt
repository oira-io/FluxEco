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

package io.oira.fluxeco.core.data.manager

import io.oira.fluxeco.core.FluxEco
import io.oira.fluxeco.core.data.DatabaseManager
import io.oira.fluxeco.core.data.model.Transaction
import io.oira.fluxeco.core.data.model.TransactionType
import io.oira.fluxeco.core.data.mongodb.repository.MongoTransactionRepository
import io.oira.fluxeco.core.data.table.Transactions
import io.oira.fluxeco.core.util.Threads
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import java.util.concurrent.CompletableFuture

object TransactionDataManager {

    private fun generateRandomId(length: Int, useUppercase: Boolean, useLowercase: Boolean, useNumbers: Boolean): String {
        val chars = buildString {
            if (useUppercase) append("ABCDEFGHIJKLMNOPQRSTUVWXYZ")
            if (useLowercase) append("abcdefghijklmnopqrstuvwxyz")
            if (useNumbers) append("0123456789")
        }

        if (chars.isEmpty()) {
            throw IllegalArgumentException("At least one character type must be enabled for random ID generation")
        }

        return (1..length)
            .map { chars.random() }
            .joinToString("")
    }

    private fun generateTransactionId(): Int {
        val plugin = FluxEco.instance
        val idFormat = plugin.config.getString("general.id-format", "sequential") ?: "sequential"

        return when (idFormat.lowercase()) {
            "random" -> {
                val randomConfig = plugin.config.getConfigurationSection("general.random-format")
                val length = randomConfig?.getInt("length", 10) ?: 10
                val useUppercase = randomConfig?.getBoolean("use-uppercase", true) ?: true
                val useLowercase = randomConfig?.getBoolean("use-lowercase", true) ?: true
                val useNumbers = randomConfig?.getBoolean("use-numbers", true) ?: true

                var id: Int
                var attempts = 0
                do {
                    val randomString = generateRandomId(length, useUppercase, useLowercase, useNumbers)
                    id = kotlin.math.abs(randomString.hashCode())
                    attempts++
                    val exists = transaction(DatabaseManager.getDatabase()) {
                        Transactions.selectAll().where { Transactions.id eq id }.count() > 0
                    }
                    if (!exists) break
                } while (attempts < 100)

                if (attempts >= 100) {
                    throw IllegalStateException("Failed to generate unique random transaction ID after 100 attempts")
                }
                id
            }
            else -> {
                transaction(DatabaseManager.getDatabase()) {
                    val maxId = Transactions.selectAll()
                        .maxOfOrNull { it[Transactions.id] } ?: 0
                    maxId + 1
                }
            }
        }
    }

    fun getTransactions(uuid: UUID): List<Transaction> = if (DatabaseManager.isMongoDB()) {
        MongoTransactionRepository.getTransactions(uuid)
    } else {
        transaction(DatabaseManager.getDatabase()) {
            Transactions.selectAll().where { Transactions.playerUuid eq uuid.toString() }
                .orderBy(Transactions.date, SortOrder.DESC)
                .map {
                Transaction(
                    id = it[Transactions.id],
                    playerUuid = UUID.fromString(it[Transactions.playerUuid]),
                    type = TransactionType.valueOf(it[Transactions.type]),
                    amount = it[Transactions.amount],
                    senderUuid = UUID.fromString(it[Transactions.senderUuid]),
                    receiverUuid = UUID.fromString(it[Transactions.receiverUuid]),
                    date = it[Transactions.date]
                )
            }
        }
    }

    fun getTransactionsAsync(uuid: UUID): CompletableFuture<List<Transaction>> {
        val future = CompletableFuture<List<Transaction>>()
        Threads.runAsync {
            try {
                val result = getTransactions(uuid)
                future.complete(result)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }
        return future
    }

    fun createTransaction(playerUuid: UUID, type: TransactionType, amount: Double, senderUuid: UUID, receiverUuid: UUID): Int {
        val transactionId = generateTransactionId()

        return if (DatabaseManager.isMongoDB()) {
            MongoTransactionRepository.createTransaction(transactionId, playerUuid, type, amount, senderUuid, receiverUuid)
        } else {
            transaction(DatabaseManager.getDatabase()) {
                Transactions.insert {
                    it[id] = transactionId
                    it[Transactions.playerUuid] = playerUuid.toString()
                    it[Transactions.type] = type.name
                    it[Transactions.amount] = amount
                    it[Transactions.senderUuid] = senderUuid.toString()
                    it[Transactions.receiverUuid] = receiverUuid.toString()
                    it[Transactions.date] = System.currentTimeMillis()
                } get Transactions.id
            }
        }
    }

    fun createTransactionAsync(playerUuid: UUID, type: TransactionType, amount: Double, senderUuid: UUID, receiverUuid: UUID): CompletableFuture<Int> {
        val future = CompletableFuture<Int>()
        Threads.runAsync {
            try {
                val result = createTransaction(playerUuid, type, amount, senderUuid, receiverUuid)
                future.complete(result)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }
        return future
    }

    fun deleteTransactions(uuid: UUID): Int = if (DatabaseManager.isMongoDB()) {
        MongoTransactionRepository.getTransactions(uuid).size.also {
            MongoTransactionRepository.getTransactions(uuid).forEach { transaction ->
                MongoTransactionRepository.deleteTransaction(transaction.id)
            }
        }
    } else {
        transaction(DatabaseManager.getDatabase()) {
            Transactions.deleteWhere { Transactions.playerUuid eq uuid.toString() }
        }
    }

    fun deleteAllTransactions(): Int = if (DatabaseManager.isMongoDB()) {
        MongoTransactionRepository.deleteAllTransactions()
    } else {
        transaction(DatabaseManager.getDatabase()) {
            Transactions.deleteAll()
        }
    }
}
