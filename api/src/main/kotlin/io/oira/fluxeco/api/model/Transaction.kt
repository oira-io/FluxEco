package io.oira.fluxeco.api.model

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

