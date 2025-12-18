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

