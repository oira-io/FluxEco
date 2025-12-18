package io.oira.fluxeco.core.data.mongodb.document

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import java.util.*

@Serializable
data class TransactionDocument(
    @BsonId
    val id: Int,
    val playerUuid: String,
    val type: String,
    val amount: Double,
    val senderUuid: String,
    val receiverUuid: String,
    val date: Long
) {
    companion object {
        fun create(
            id: Int,
            playerUuid: UUID,
            type: String,
            amount: Double,
            senderUuid: UUID,
            receiverUuid: UUID,
            date: Long = System.currentTimeMillis()
        ) = TransactionDocument(
            id = id,
            playerUuid = playerUuid.toString(),
            type = type,
            amount = amount,
            senderUuid = senderUuid.toString(),
            receiverUuid = receiverUuid.toString(),
            date = date
        )
    }

    fun getPlayerUUID(): UUID = UUID.fromString(playerUuid)
    fun getSenderUUID(): UUID = UUID.fromString(senderUuid)
    fun getReceiverUUID(): UUID = UUID.fromString(receiverUuid)
}

