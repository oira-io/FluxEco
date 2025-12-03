package io.oira.fluxeco.api.model

import java.util.UUID

data class PlayerSession(
    val uuid: UUID,
    val name: String,
    val serverId: String
)
