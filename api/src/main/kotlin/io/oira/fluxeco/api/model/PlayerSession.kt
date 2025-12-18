package io.oira.fluxeco.api.model

import java.util.*

data class PlayerSession(
    val uuid: UUID,
    val name: String,
    val serverId: String
)
