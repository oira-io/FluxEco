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
    /**
 * Retrieve the MongoDB collection used to store player profiles.
 *
 * @return The player profiles MongoDB collection instance.
 */
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
