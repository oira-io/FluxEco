/*
 * FluxEco
 * Copyright (C) 2025 Harfull
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package io.oira.fluxeco.core.lamp

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.jetbrains.annotations.Blocking
import revxrsal.commands.autocomplete.SuggestionProvider
import revxrsal.commands.bukkit.exception.InvalidPlayerException
import revxrsal.commands.command.CommandActor
import revxrsal.commands.node.ExecutionContext
import revxrsal.commands.parameter.ParameterType
import revxrsal.commands.stream.MutableStringStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

abstract class AsyncOfflinePlayer {

    abstract fun getName(): String
    abstract fun getIfFetched(): OfflinePlayer?
    @Blocking
    abstract fun getOrFetch(): OfflinePlayer
    abstract fun getOrFetchAsync(): CompletableFuture<OfflinePlayer>
    abstract fun hasPlayedBeforeAsync(): CompletableFuture<Boolean>

    enum class HasPlayedBefore {
        YES, NO, UNKNOWN;

        fun get(): Boolean {
            return when (this) {
                YES -> true
                NO -> false
                UNKNOWN -> throw IllegalStateException("Status unknown.")
            }
        }

        fun getOrNull(): Boolean? {
            return when (this) {
                YES -> true
                NO -> false
                UNKNOWN -> null
            }
        }
    }

    open fun hasPlayedBefore(): HasPlayedBefore {
        val future = hasPlayedBeforeAsync()
        return if (future.isDone) {
            try {
                if (future.get()) HasPlayedBefore.YES else HasPlayedBefore.NO
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        } else HasPlayedBefore.UNKNOWN
    }

    companion object {
        private val PLAYER_CACHE: Cache<String, AsyncOfflinePlayer> = CacheBuilder.newBuilder()
            .expireAfterAccess(6, TimeUnit.HOURS)
            .build()

        private val ongoingFetches: MutableMap<String, CompletableFuture<OfflinePlayer>> = ConcurrentHashMap()

        @JvmStatic
        fun from(name: String): AsyncOfflinePlayer {
            val player = Bukkit.getPlayer(name)
            if (player != null) return Resolved(player)
            val cached = PLAYER_CACHE.getIfPresent(name)
            if (cached != null) return cached
            val ongoingFuture = ongoingFetches[name]
            if (ongoingFuture != null) return Fetching(name, ongoingFuture)
            val future = CompletableFuture.supplyAsync { Bukkit.getOfflinePlayer(name) }
            ongoingFetches[name] = future
            future.thenAccept { offlinePlayer ->
                PLAYER_CACHE.put(name, Resolved(offlinePlayer))
                ongoingFetches.remove(name)
            }
            return Fetching(name, future)
        }

        @JvmStatic
        fun from(player: OfflinePlayer): AsyncOfflinePlayer {
            return Resolved(player)
        }

        @JvmStatic
        fun parameterType(): ParameterType<CommandActor, AsyncOfflinePlayer> {
            return LampParameterType
        }
    }

    private class Resolved(private val player: OfflinePlayer) : AsyncOfflinePlayer() {
        override fun getName(): String = player.name ?: throw IllegalStateException("player.getName() is null!")
        override fun getIfFetched(): OfflinePlayer = player
        override fun getOrFetch(): OfflinePlayer = player
        override fun getOrFetchAsync(): CompletableFuture<OfflinePlayer> = CompletableFuture.completedFuture(player)
        override fun hasPlayedBefore(): HasPlayedBefore =
            if (player.hasPlayedBefore()) HasPlayedBefore.YES else HasPlayedBefore.NO

        override fun hasPlayedBeforeAsync(): CompletableFuture<Boolean> =
            CompletableFuture.completedFuture(player.hasPlayedBefore())
    }

    private class Fetching(private val name: String, private val future: CompletableFuture<OfflinePlayer>) : AsyncOfflinePlayer() {
        override fun getName(): String = name

        override fun getIfFetched(): OfflinePlayer? {
            return if (future.isDone) {
                try {
                    future.get()
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }
            } else null
        }

        override fun getOrFetch(): OfflinePlayer {
            return try {
                future.get()
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }

        override fun getOrFetchAsync(): CompletableFuture<OfflinePlayer> = future
        override fun hasPlayedBeforeAsync(): CompletableFuture<Boolean> = future.thenApply { it.hasPlayedBefore() }
    }

    private object LampParameterType : ParameterType<CommandActor, AsyncOfflinePlayer> {

        override fun parse(input: MutableStringStream, context: ExecutionContext<CommandActor>): AsyncOfflinePlayer {
            val name = input.readUnquotedString()
            val player = from(name)
            if (player.hasPlayedBefore() == HasPlayedBefore.NO) throw InvalidPlayerException(name)
            return player
        }

        override fun defaultSuggestions(): SuggestionProvider<CommandActor> {
            return SuggestionProvider { _ ->
                val names = HashSet<String>()
                for (player in Bukkit.getOnlinePlayers()) names.add(player.name)
                names.addAll(PLAYER_CACHE.asMap().keys)
                names
            }
        }
    }
}
