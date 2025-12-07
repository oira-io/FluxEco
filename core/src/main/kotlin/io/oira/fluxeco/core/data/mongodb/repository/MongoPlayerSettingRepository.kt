package io.oira.fluxeco.core.data.mongodb.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import io.oira.fluxeco.core.data.model.PlayerSetting
import io.oira.fluxeco.core.data.mongodb.MongoDBManager
import io.oira.fluxeco.core.data.mongodb.document.PlayerSettingDocument
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import java.util.UUID

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

