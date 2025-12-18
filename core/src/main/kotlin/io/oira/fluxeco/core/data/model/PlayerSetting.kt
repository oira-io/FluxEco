package io.oira.fluxeco.core.data.model

import java.util.*

data class PlayerSetting(
    val uuid: UUID,
    val togglePayments: Boolean,
    val payAlerts: Boolean
)
