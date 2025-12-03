package io.oira.fluxeco.core.util

import io.github.miniplaceholders.api.MiniPlaceholders
import me.clip.placeholderapi.PlaceholderAPI
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.text.DecimalFormat

@Suppress("unused")
class Placeholders {

    private val placeholders = mutableMapOf<String, String>()
    private var contextPlayer: Player? = null

    companion object {
        fun builder(): Builder = Builder()

        fun create(vararg pairs: Pair<String, Any>): Placeholders {
            return builder().apply {
                pairs.forEach { (key, value) -> add(key, value) }
            }.build()
        }
    }

    class Builder {
        private val placeholders = mutableMapOf<String, String>()

        fun add(key: String, value: Any): Builder {
            val placeholder = if (key.startsWith("{") && key.endsWith("}")) {
                key
            } else {
                "{$key}"
            }
            placeholders[placeholder] = value.toString()
            return this
        }

        fun add(key: String, value: String): Builder {
            val placeholder = if (key.startsWith("{") && key.endsWith("}")) {
                key
            } else {
                "{$key}"
            }
            placeholders[placeholder] = value
            return this
        }

        fun add(key: String, value: Int): Builder = add(key, value.toString())

        fun add(key: String, value: Long): Builder = add(key, value.toString())

        fun add(key: String, value: Double, format: String = "#.##"): Builder {
            val formatter = DecimalFormat(format)
            return add(key, formatter.format(value))
        }

        fun add(key: String, value: Boolean): Builder = add(key, value.toString())

        fun addPlayer(player: Player): Builder {
            add("player", player.name)
            add("player_uuid", player.uniqueId.toString())
            add("player_displayname", player.displayName())
            return this
        }

        fun addPlayerCustom(prefix: String, player: Player): Builder {
            add("${prefix}_name", player.name)
            add("${prefix}_uuid", player.uniqueId.toString())
            add("${prefix}_displayname", player.displayName())
            return this
        }

        fun addAll(map: Map<String, Any>): Builder {
            map.forEach { (key, value) -> add(key, value) }
            return this
        }

        fun addAll(other: Placeholders): Builder {
            placeholders.putAll(other.placeholders)
            return this
        }

        fun build(): Placeholders {
            return Placeholders().apply {
                this.placeholders.putAll(this@Builder.placeholders)
            }
        }
    }

    fun add(key: String, value: Any): Placeholders {
        val placeholder = if (key.startsWith("{") && key.endsWith("}")) {
            key
        } else {
            "{$key}"
        }
        placeholders[placeholder] = value.toString()
        return this
    }

    fun setPlayer(player: Player?): Placeholders {
        contextPlayer = player
        return this
    }

    fun replace(text: String): String {
        var result = text

        placeholders.forEach { (key, value) ->
            result = result.replace(key, value, ignoreCase = true)
        }

        if (contextPlayer != null && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                result = PlaceholderAPI.setPlaceholders(contextPlayer!!, result)
            } catch (_: Exception) {}
        }

        contextPlayer?.let { player ->
            if (Bukkit.getPluginManager().isPluginEnabled("MiniPlaceholders")) {
                try {
                    val audienceResolver = MiniPlaceholders.audiencePlaceholders()
                    val component = MiniMessage.miniMessage().deserialize(result, player, audienceResolver)
                    result = LegacyComponentSerializer.legacySection().serialize(component)
                } catch (_: Exception) {}
            }
        }

        return result
    }

    fun getPlaceholders(): Map<String, String> = placeholders.toMap()

    fun toMap(): Map<String, String> = placeholders.toMap()

    fun isEmpty(): Boolean = placeholders.isEmpty()

    fun isNotEmpty(): Boolean = placeholders.isNotEmpty()

    fun size(): Int = placeholders.size

    fun clear() {
        placeholders.clear()
    }

    override fun toString(): String {
        return "Placeholders(${placeholders.entries.joinToString(", ") { "${it.key}=${it.value}" }})"
    }
}

fun Player.toPlaceholders(prefix: String = "player"): Placeholders {
    return Placeholders.builder()
        .add("${prefix}_name", this.name)
        .add("${prefix}_uuid", this.uniqueId.toString())
        .add("${prefix}_displayname", this.displayName())
        .build()
}

fun Map<String, Any>.toPlaceholders(): Placeholders {
    return Placeholders.builder()
        .addAll(this)
        .build()
}