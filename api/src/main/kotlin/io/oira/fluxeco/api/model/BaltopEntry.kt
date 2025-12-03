package io.oira.fluxeco.api.model

import java.util.UUID

data class BaltopEntry(
    val uuid: UUID,
    val name: String,
    val balance: Double
)

