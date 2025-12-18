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

package io.oira.fluxeco.core.data.mongodb.document

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import java.util.*

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

