package io.oira.fluxeco.api.event

import org.bukkit.event.HandlerList
import java.util.*

class BalanceChangedEvent(
    val uuid: UUID,
    val oldBalance: Double,
    val newBalance: Double
) : EconomyEvent() {

    val delta: Double
        get() = newBalance - oldBalance

    companion object {
        @JvmStatic
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }

    override fun getHandlers(): HandlerList = Companion.handlers
}

