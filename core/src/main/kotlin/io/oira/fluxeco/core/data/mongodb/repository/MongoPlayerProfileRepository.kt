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
import io.oira.fluxeco.core.data.model.PlayerProfile
import io.oira.fluxeco.core.data.mongodb.MongoDBManager
import io.oira.fluxeco.core.data.mongodb.document.PlayerProfileDocument
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import java.util.*

object MongoPlayerProfileRepository {
    private fun getCollection() = MongoDBManager.getPlayerProfilesCollection()

    fun getProfile(uuid: UUID): PlayerProfile? = runBlocking {
        val doc = getCollection().find(Filters.eq("_id", uuid.toString())).firstOrNull()
        doc?.let {
            PlayerProfile(
                uuid = it.getUUID(),
                name = it.name,
                skinUrl = it.skinUrl,
                capeUrl = it.capeUrl,
                updatedAt = it.updatedAt
            )
        }
    }

    fun setProfile(uuid: UUID, name: String, skinUrl: String?, capeUrl: String?) = runBlocking {
        val doc = PlayerProfileDocument.fromUUID(
            uuid = uuid,
            name = name,
            skinUrl = skinUrl,
            capeUrl = capeUrl,
            updatedAt = System.currentTimeMillis()
        )
        getCollection().replaceOne(
            Filters.eq("_id", uuid.toString()),
            doc,
            ReplaceOptions().upsert(true)
        )
    }

    fun getSkinUrl(uuid: UUID): String? = getProfile(uuid)?.skinUrl

    fun getCapeUrl(uuid: UUID): String? = getProfile(uuid)?.capeUrl
}

