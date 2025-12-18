package io.oira.fluxeco.api.event

import org.bukkit.event.HandlerList
import java.util.*

class PlayerTransferredEvent(
    val from: UUID,
    val to: UUID,
    val amount: Double
) : EconomyEvent() {

    companion object {
        @JvmStatic
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }

    override fun getHandlers(): HandlerList = Companion.handlers
}

