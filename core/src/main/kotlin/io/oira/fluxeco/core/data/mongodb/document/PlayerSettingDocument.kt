package io.oira.fluxeco.core.data.mongodb.document

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import java.util.UUID

@Serializable
data class PlayerSettingDocument(
    @BsonId
    val uuid: String,
    val togglePayments: Boolean = true,
    val payAlerts: Boolean = true
) {
    companion object {
        fun fromUUID(
            uuid: UUID,
            togglePayments: Boolean = true,
            payAlerts: Boolean = true
        ) = PlayerSettingDocument(
            uuid = uuid.toString(),
            togglePayments = togglePayments,
            payAlerts = payAlerts
        )
    }

    fun getUUID(): UUID = UUID.fromString(uuid)
}

