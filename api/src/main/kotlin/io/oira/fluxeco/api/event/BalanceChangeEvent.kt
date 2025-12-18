package io.oira.fluxeco.api.event

import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList
import java.util.*

class BalanceChangeEvent(
    val uuid: UUID,
    val oldBalance: Double,
    var newBalance: Double
) : EconomyEvent(), Cancellable {

    private var cancelled = false

    val delta: Double
        get() = newBalance - oldBalance

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    companion object {
        @JvmStatic
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }

    override fun getHandlers(): HandlerList = Companion.handlers
}

