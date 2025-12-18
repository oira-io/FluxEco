package io.oira.fluxeco.core.data.mongodb.document

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import java.util.*

@Serializable
data class BalanceDocument(
    @BsonId
    val uuid: String,
    val balance: Double = 0.0
) {
    companion object {
        fun fromUUID(uuid: UUID, balance: Double = 0.0) = BalanceDocument(
            uuid = uuid.toString(),
            balance = balance
        )
    }

    fun getUUID(): UUID = UUID.fromString(uuid)
}

