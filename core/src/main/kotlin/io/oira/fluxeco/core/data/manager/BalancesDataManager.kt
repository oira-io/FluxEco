package io.oira.fluxeco.core.data.manager

import io.oira.fluxeco.core.data.DatabaseManager
import io.oira.fluxeco.core.data.table.Balances
import io.oira.fluxeco.core.util.Threads
import io.oira.fluxeco.core.data.model.Balance
import io.oira.fluxeco.core.data.mongodb.repository.MongoBalanceRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.CompletableFuture
import java.util.UUID

object BalancesDataManager {
    fun getAllBalances(): List<Balance> = transaction(DatabaseManager.getDatabase()) {
        Balances.selectAll().map {
            Balance(
                uuid = UUID.fromString(it[Balances.uuid]),
                balance = it[Balances.balance]
            )
        }
    }

    fun getAllBalancesAsync(): CompletableFuture<List<Balance>> {
        val future = CompletableFuture<List<Balance>>()
        Threads.runAsync {
            try {
                val result = getAllBalances()
                future.complete(result)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }
        return future
    }

    fun getBalance(uuid: UUID): Balance? = transaction(DatabaseManager.getDatabase()) {
        Balances.selectAll().where { Balances.uuid eq uuid.toString() }
            .map {
                Balance(
                    uuid = UUID.fromString(it[Balances.uuid]),
                    balance = it[Balances.balance]
                )
            }
            .singleOrNull()
    }

    fun createBalance(uuid: UUID, balance: Double) = transaction(DatabaseManager.getDatabase()) {
        Balances.insert {
            it[Balances.uuid] = uuid.toString()
            it[Balances.balance] = balance
        }
    }

    fun createBalanceAsync(uuid: UUID, balance: Double): CompletableFuture<Unit> {
        val future = CompletableFuture<Unit>()
        Threads.runAsync {
            try {
                createBalance(uuid, balance)
                future.complete(Unit)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }
        return future
    }

    fun updateBalance(uuid: UUID, balance: Double): Int = transaction(DatabaseManager.getDatabase()) {
        Balances.replace {
            it[Balances.uuid] = uuid.toString()
            it[Balances.balance] = balance
        }
        1
    }

    fun updateBalanceAsync(uuid: UUID, balance: Double): CompletableFuture<Int> {
        val future = CompletableFuture<Int>()
        Threads.runAsync {
            try {
                val result = updateBalance(uuid, balance)
                future.complete(result)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }
        return future
    }

    fun deleteBalance(uuid: UUID): Int = if (DatabaseManager.isMongoDB()) {
        MongoBalanceRepository.deleteBalance(uuid)
    } else {
        transaction(DatabaseManager.getDatabase()) {
            Balances.deleteWhere { Balances.uuid eq uuid.toString() }
        }
    }

    fun deleteAllBalances(): Int = if (DatabaseManager.isMongoDB()) {
        MongoBalanceRepository.deleteAllBalances()
    } else {
        transaction(DatabaseManager.getDatabase()) {
            Balances.deleteAll()
        }
    }

    fun deleteAllBalancesAsync(): CompletableFuture<Int> {
        val future = CompletableFuture<Int>()
        Threads.runAsync {
            try {
                val result = deleteAllBalances()
                future.complete(result)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }
        return future
    }

    fun setBalanceAsync(uuid: UUID, balance: Double): CompletableFuture<Unit> {
        val future = CompletableFuture<Unit>()
        Threads.runAsync {
            try {
                transaction(DatabaseManager.getDatabase()) {
                    Balances.deleteAll()
                    Balances.insert {
                        it[Balances.uuid] = uuid.toString()
                        it[Balances.balance] = balance
                    }
                    future.complete(Unit)
                }
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }
        return future
    }
}
