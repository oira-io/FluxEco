package io.oira.fluxeco.core.data.model

import java.util.*

data class PlayerProfile(
    val uuid: UUID,
    val name: String,
    val skinUrl: String?,
    val capeUrl: String?,
    val updatedAt: Long
)
