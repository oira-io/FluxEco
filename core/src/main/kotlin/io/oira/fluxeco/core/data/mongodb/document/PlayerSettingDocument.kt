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

