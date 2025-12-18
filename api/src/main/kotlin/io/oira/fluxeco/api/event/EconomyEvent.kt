package io.oira.fluxeco.api.event

import org.bukkit.event.Event
import org.bukkit.event.HandlerList

abstract class EconomyEvent : Event() {
    companion object {
        @JvmStatic
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }

    override fun getHandlers(): HandlerList = Companion.handlers
}

