package io.oira.fluxeco.core.data.manager

import io.oira.fluxeco.core.data.DatabaseManager
import io.oira.fluxeco.core.data.model.PlayerProfile
import io.oira.fluxeco.core.data.mongodb.repository.MongoPlayerProfileRepository
import io.oira.fluxeco.core.data.table.PlayerProfiles
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

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
