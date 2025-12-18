package io.oira.fluxeco.api.event

import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList
import java.util.*

class PlayerTransferEvent(
    val from: UUID,
    val to: UUID,
    var amount: Double
) : EconomyEvent(), Cancellable {

    private var cancelled = false

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

