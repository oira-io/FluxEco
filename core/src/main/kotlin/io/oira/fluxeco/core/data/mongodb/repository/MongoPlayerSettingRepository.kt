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

package io.oira.fluxeco.core.data.mongodb.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import io.oira.fluxeco.core.data.model.PlayerSetting
import io.oira.fluxeco.core.data.mongodb.MongoDBManager
import io.oira.fluxeco.core.data.mongodb.document.PlayerSettingDocument
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import java.util.*

object MongoPlayerSettingRepository {
    private fun getCollection() = MongoDBManager.getPlayerSettingsCollection()

    fun getSetting(uuid: UUID): PlayerSetting? = runBlocking {
        val doc = getCollection().find(Filters.eq("_id", uuid.toString())).firstOrNull()
        doc?.let {
            PlayerSetting(
                uuid = it.getUUID(),
                togglePayments = it.togglePayments,
                payAlerts = it.payAlerts
            )
        }
    }

    fun setTogglePayments(uuid: UUID, togglePayments: Boolean): Int = runBlocking {
        val current = getSetting(uuid)
        val doc = PlayerSettingDocument.fromUUID(
            uuid = uuid,
            togglePayments = togglePayments,
            payAlerts = current?.payAlerts ?: true
        )
        val result = getCollection().replaceOne(
            Filters.eq("_id", uuid.toString()),
            doc,
            ReplaceOptions().upsert(true)
        )
        if (result.modifiedCount > 0 || result.upsertedId != null) 1 else 0
    }

    fun setPayAlerts(uuid: UUID, payAlerts: Boolean): Int = runBlocking {
        val current = getSetting(uuid)
        val doc = PlayerSettingDocument.fromUUID(
            uuid = uuid,
            togglePayments = current?.togglePayments ?: true,
            payAlerts = payAlerts
        )
        val result = getCollection().replaceOne(
            Filters.eq("_id", uuid.toString()),
            doc,
            ReplaceOptions().upsert(true)
        )
        if (result.modifiedCount > 0 || result.upsertedId != null) 1 else 0
    }

    fun getTogglePayments(uuid: UUID): Boolean = getSetting(uuid)?.togglePayments ?: true

    fun getPayAlerts(uuid: UUID): Boolean = getSetting(uuid)?.payAlerts ?: true
}

