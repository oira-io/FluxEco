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

package io.oira.fluxeco.core.data.manager

import io.oira.fluxeco.core.data.DatabaseManager
import io.oira.fluxeco.core.data.model.PlayerProfile
import io.oira.fluxeco.core.data.mongodb.repository.MongoPlayerProfileRepository
import io.oira.fluxeco.core.data.table.PlayerProfiles
import org.jetbrains.exposed.sql.replace
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object PlayerProfileManager {

    fun getProfile(uuid: UUID): PlayerProfile? = if (DatabaseManager.isMongoDB()) {
        MongoPlayerProfileRepository.getProfile(uuid)
    } else {
        transaction(DatabaseManager.getDatabase()) {
            PlayerProfiles.selectAll().where { PlayerProfiles.uuid eq uuid.toString() }
                .map {
                    PlayerProfile(
                        uuid = UUID.fromString(it[PlayerProfiles.uuid]),
                        name = it[PlayerProfiles.name],
                        skinUrl = it[PlayerProfiles.skinUrl],
                        capeUrl = it[PlayerProfiles.capeUrl],
                        updatedAt = it[PlayerProfiles.updatedAt]
                    )
                }
                .singleOrNull()
        }
    }

    fun setProfile(uuid: UUID, name: String, skinUrl: String?, capeUrl: String?) {
        if (DatabaseManager.isMongoDB()) {
            MongoPlayerProfileRepository.setProfile(uuid, name, skinUrl, capeUrl)
        } else {
            transaction(DatabaseManager.getDatabase()) {
                PlayerProfiles.replace {
                    it[PlayerProfiles.uuid] = uuid.toString()
                    it[PlayerProfiles.name] = name
                    it[PlayerProfiles.skinUrl] = skinUrl
                    it[PlayerProfiles.capeUrl] = capeUrl
                    it[PlayerProfiles.updatedAt] = System.currentTimeMillis()
                }
            }
        }
    }

    fun getSkinUrl(uuid: UUID): String? {
        return getProfile(uuid)?.skinUrl
    }

    fun getCapeUrl(uuid: UUID): String? {
        return getProfile(uuid)?.capeUrl
    }
}
