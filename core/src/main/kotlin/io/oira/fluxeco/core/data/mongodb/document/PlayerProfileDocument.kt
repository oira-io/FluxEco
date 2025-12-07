package io.oira.fluxeco.core.data.mongodb.document

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import java.util.UUID

@Serializable
data class PlayerProfileDocument(
    @BsonId
    val uuid: String,
    val name: String,
    val skinUrl: String? = null,
    val capeUrl: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromUUID(
            uuid: UUID,
            name: String,
            skinUrl: String? = null,
            capeUrl: String? = null,
            updatedAt: Long = System.currentTimeMillis()
        ) = PlayerProfileDocument(
            uuid = uuid.toString(),
            name = name,
            skinUrl = skinUrl,
            capeUrl = capeUrl,
            updatedAt = updatedAt
        )
    }

    fun getUUID(): UUID = UUID.fromString(uuid)
}

