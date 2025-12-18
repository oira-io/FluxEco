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

package io.oira.fluxeco.core.integration

import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.io.*
import java.lang.reflect.Method
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.Supplier
import java.util.logging.Level
import java.util.zip.GZIPOutputStream
import javax.net.ssl.HttpsURLConnection

class Metrics(private val plugin: Plugin, serviceId: Int) {

    private val metricsBase: MetricsBase

    init {
        val bStatsFolder = File(plugin.dataFolder.parentFile, "bStats")
        val configFile = File(bStatsFolder, "config.yml")
        val config = YamlConfiguration.loadConfiguration(configFile)
        if (!config.isSet("serverUuid")) {
            config.addDefault("enabled", true)
            config.addDefault("serverUuid", UUID.randomUUID().toString())
            config.addDefault("logFailedRequests", false)
            config.addDefault("logSentData", false)
            config.addDefault("logResponseStatusText", false)
            config.options().header(
                "bStats (https://bStats.org) collects some basic information for plugin authors, like how\n" +
                        "many people use their plugin and their total player count. It's recommended to keep bStats\n" +
                        "enabled, but if you're not comfortable with this, you can turn this setting off. There is no\n" +
                        "performance penalty associated with having metrics enabled, and data sent to bStats is fully\n" +
                        "anonymous."
            ).copyDefaults(true)
            try {
                config.save(configFile)
            } catch (_: IOException) {
            }
        }
        val enabled = config.getBoolean("enabled", true)
        val serverUUID = config.getString("serverUuid")
        val logErrors = config.getBoolean("logFailedRequests", false)
        val logSentData = config.getBoolean("logSentData", false)
        val logResponseStatusText = config.getBoolean("logResponseStatusText", false)
        var isFolia = false
        try {
            isFolia = Class.forName("io.papermc.paper.threadedregions.RegionizedServer") != null
        } catch (_: Exception) {
        }
        metricsBase = MetricsBase(
            "bukkit",
            serverUUID!!,
            serviceId,
            enabled,
            ::appendPlatformData,
            ::appendServiceData,
            if (!isFolia) { submitDataTask: Runnable -> Bukkit.getScheduler().runTask(plugin, submitDataTask) } else null,
            { plugin.isEnabled },
            { message, error -> this.plugin.logger.log(Level.WARNING, message, error) },
            { message -> this.plugin.logger.log(Level.INFO, message) },
            logErrors,
            logSentData,
            logResponseStatusText,
            false
        )
    }

    fun shutdown() {
        metricsBase.shutdown()
    }

    fun addCustomChart(chart: CustomChart) {
        metricsBase.addCustomChart(chart)
    }

    private fun appendPlatformData(builder: JsonObjectBuilder) {
        builder.appendField("playerAmount", getPlayerAmount())
        builder.appendField("onlineMode", if (Bukkit.getOnlineMode()) 1 else 0)
        builder.appendField("bukkitVersion", Bukkit.getVersion())
        builder.appendField("bukkitName", Bukkit.getName())
        builder.appendField("javaVersion", System.getProperty("java.version"))
        builder.appendField("osName", System.getProperty("os.name"))
        builder.appendField("osArch", System.getProperty("os.arch"))
        builder.appendField("osVersion", System.getProperty("os.version"))
        builder.appendField("coreCount", Runtime.getRuntime().availableProcessors())
    }

    private fun appendServiceData(builder: JsonObjectBuilder) {
        builder.appendField("pluginVersion", plugin.description.version)
    }

    private fun getPlayerAmount(): Int {
        return try {
            val onlinePlayersMethod: Method = Class.forName("org.bukkit.Server").getMethod("getOnlinePlayers")
            if (onlinePlayersMethod.returnType == Collection::class.java) {
                (onlinePlayersMethod.invoke(Bukkit.getServer()) as Collection<*>).size
            } else {
                (onlinePlayersMethod.invoke(Bukkit.getServer()) as Array<Player>).size
            }
        } catch (_: Exception) {
            Bukkit.getOnlinePlayers().size
        }
    }

    class MetricsBase(
        private val platform: String,
        private val serverUuid: String,
        private val serviceId: Int,
        private val enabled: Boolean,
        private val appendPlatformDataConsumer: Consumer<JsonObjectBuilder>,
        private val appendServiceDataConsumer: Consumer<JsonObjectBuilder>,
        private val submitTaskConsumer: Consumer<Runnable>?,
        private val checkServiceEnabledSupplier: Supplier<Boolean>,
        private val errorLogger: BiConsumer<String, Throwable>,
        private val infoLogger: Consumer<String>,
        private val logErrors: Boolean,
        private val logSentData: Boolean,
        private val logResponseStatusText: Boolean,
        skipRelocateCheck: Boolean
    ) {

        companion object {
            const val METRICS_VERSION = "3.1.0"
            private const val REPORT_URL = "https://bStats.org/api/v2/data/%s"
        }

        private val scheduler: ScheduledExecutorService = ScheduledThreadPoolExecutor(
            1
        ) { task ->
            Thread(task, "bStats-Metrics").apply { isDaemon = true }
        }

        private val customCharts: MutableSet<CustomChart> = HashSet()

        init {
            (scheduler as ScheduledThreadPoolExecutor).setExecuteExistingDelayedTasksAfterShutdownPolicy(false)
            if (!skipRelocateCheck) {
                checkRelocation()
            }
            if (enabled) {
                startSubmitting()
            }
        }

        fun addCustomChart(chart: CustomChart) {
            customCharts.add(chart)
        }

        fun shutdown() {
            scheduler.shutdown()
        }

        private fun startSubmitting() {
            val submitTask = Runnable {
                if (!enabled || !checkServiceEnabledSupplier.get()) {
                    scheduler.shutdown()
                    return@Runnable
                }
                if (submitTaskConsumer != null) {
                    submitTaskConsumer.accept(Runnable { submitData() })
                } else {
                    submitData()
                }
            }
            val initialDelay = (1000 * 60 * (3 + Math.random() * 3)).toLong()
            val secondDelay = (1000 * 60 * (Math.random() * 30)).toLong()
            scheduler.schedule(submitTask, initialDelay, TimeUnit.MILLISECONDS)
            scheduler.scheduleAtFixedRate(submitTask, initialDelay + secondDelay, 1000 * 60 * 30, TimeUnit.MILLISECONDS)
        }

        private fun submitData() {
            val baseJsonBuilder = JsonObjectBuilder()
            appendPlatformDataConsumer.accept(baseJsonBuilder)
            val serviceJsonBuilder = JsonObjectBuilder()
            appendServiceDataConsumer.accept(serviceJsonBuilder)
            val chartData = customCharts.mapNotNull { it.getRequestJsonObject(errorLogger, logErrors) }.toTypedArray()
            serviceJsonBuilder.appendField("id", serviceId)
            serviceJsonBuilder.appendField("customCharts", chartData)
            baseJsonBuilder.appendField("service", serviceJsonBuilder.build())
            baseJsonBuilder.appendField("serverUUID", serverUuid)
            baseJsonBuilder.appendField("metricsVersion", METRICS_VERSION)
            val data = baseJsonBuilder.build()
            scheduler.execute {
                try {
                    sendData(data)
                } catch (e: Exception) {
                    if (logErrors) {
                        errorLogger.accept("Could not submit bStats metrics data", e)
                    }
                }
            }
        }

        private fun sendData(data: JsonObjectBuilder.JsonObject) {
            if (logSentData) {
                infoLogger.accept("Sent bStats metrics data: $data")
            }
            val url = String.format(REPORT_URL, platform)
            val connection = URL(url).openConnection() as HttpsURLConnection
            val compressedData = compress(data.toString())
            connection.requestMethod = "POST"
            connection.addRequestProperty("Accept", "application/json")
            connection.addRequestProperty("Connection", "close")
            connection.addRequestProperty("Content-Encoding", "gzip")
            connection.addRequestProperty("Content-Length", compressedData.size.toString())
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("User-Agent", "Metrics-Service/1")
            connection.doOutput = true
            DataOutputStream(connection.outputStream).use { it.write(compressedData) }
            val builder = StringBuilder()
            BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    builder.append(line)
                    line = reader.readLine()
                }
            }
            if (logResponseStatusText) {
                infoLogger.accept("Sent data to bStats and received response: $builder")
            }
        }

        private fun checkRelocation() {
            if (System.getProperty("bstats.relocatecheck") != "false") {
                val defaultPackage = String(byteArrayOf(111.toByte(), 114.toByte(), 103.toByte(), 46.toByte(), 98.toByte(), 115.toByte(), 116.toByte(), 97.toByte(), 116.toByte(), 115.toByte()))
                val examplePackage = String(byteArrayOf(121.toByte(), 111.toByte(), 117.toByte(), 114.toByte(), 46.toByte(), 112.toByte(), 97.toByte(), 99.toByte(), 107.toByte(), 97.toByte(), 103.toByte(), 101.toByte()))
                if (this::class.java.`package`.name.startsWith(defaultPackage) || this::class.java.`package`.name.startsWith(examplePackage)) {
                    throw IllegalStateException("bStats Metrics class has not been relocated correctly!")
                }
            }
        }

        private fun compress(str: String?): ByteArray {
            if (str == null) return ByteArray(0)
            val outputStream = ByteArrayOutputStream()
            GZIPOutputStream(outputStream).use { it.write(str.toByteArray(StandardCharsets.UTF_8)) }
            return outputStream.toByteArray()
        }
    }

    abstract class CustomChart(private val chartId: String) {

        init {
            require(chartId.isNotEmpty()) { "chartId must not be null" }
        }

        fun getRequestJsonObject(errorLogger: BiConsumer<String, Throwable>, logErrors: Boolean): JsonObjectBuilder.JsonObject? {
            val builder = JsonObjectBuilder()
            builder.appendField("chartId", chartId)
            return try {
                val data = getChartData() ?: return null
                builder.appendField("data", data)
                builder.build()
            } catch (t: Throwable) {
                if (logErrors) {
                    errorLogger.accept("Failed to get data for custom chart with id $chartId", t)
                }
                null
            }
        }

        protected abstract fun getChartData(): JsonObjectBuilder.JsonObject?
    }

    class AdvancedBarChart(chartId: String, private val callable: Callable<Map<String, IntArray>>) : CustomChart(chartId) {
        override fun getChartData(): JsonObjectBuilder.JsonObject? {
            val valuesBuilder = JsonObjectBuilder()
            val map = callable.call() ?: return null
            if (map.isEmpty()) return null
            var allSkipped = true
            for ((key, value) in map) {
                if (value.isEmpty()) continue
                allSkipped = false
                valuesBuilder.appendField(key, value)
            }
            if (allSkipped) return null
            return JsonObjectBuilder().appendField("values", valuesBuilder.build()).build()
        }
    }

    class SimplePie(chartId: String, private val callable: Callable<String>) : CustomChart(chartId) {
        override fun getChartData(): JsonObjectBuilder.JsonObject? {
            val value = callable.call()
            if (value.isNullOrEmpty()) return null
            return JsonObjectBuilder().appendField("value", value).build()
        }
    }

    class DrilldownPie(chartId: String, private val callable: Callable<Map<String, Map<String, Int>>>) : CustomChart(chartId) {
        override fun getChartData(): JsonObjectBuilder.JsonObject? {
            val valuesBuilder = JsonObjectBuilder()
            val map = callable.call() ?: return null
            if (map.isEmpty()) return null
            var reallyAllSkipped = true
            for ((key, innerMap) in map) {
                val valueBuilder = JsonObjectBuilder()
                var allSkipped = true
                for ((k, v) in innerMap) {
                    valueBuilder.appendField(k, v)
                    allSkipped = false
                }
                if (!allSkipped) {
                    reallyAllSkipped = false
                    valuesBuilder.appendField(key, valueBuilder.build())
                }
            }
            if (reallyAllSkipped) return null
            return JsonObjectBuilder().appendField("values", valuesBuilder.build()).build()
        }
    }

    class SingleLineChart(chartId: String, private val callable: Callable<Int>) : CustomChart(chartId) {
        override fun getChartData(): JsonObjectBuilder.JsonObject? {
            val value = callable.call()
            if (value == 0) return null
            return JsonObjectBuilder().appendField("value", value).build()
        }
    }

    class MultiLineChart(chartId: String, private val callable: Callable<Map<String, Int>>) : CustomChart(chartId) {
        override fun getChartData(): JsonObjectBuilder.JsonObject? {
            val valuesBuilder = JsonObjectBuilder()
            val map = callable.call() ?: return null
            if (map.isEmpty()) return null
            var allSkipped = true
            for ((key, value) in map) {
                if (value == 0) continue
                allSkipped = false
                valuesBuilder.appendField(key, value)
            }
            if (allSkipped) return null
            return JsonObjectBuilder().appendField("values", valuesBuilder.build()).build()
        }
    }

    class AdvancedPie(chartId: String, private val callable: Callable<Map<String, Int>>) : CustomChart(chartId) {
        override fun getChartData(): JsonObjectBuilder.JsonObject? {
            val valuesBuilder = JsonObjectBuilder()
            val map = callable.call() ?: return null
            if (map.isEmpty()) return null
            var allSkipped = true
            for ((key, value) in map) {
                if (value == 0) continue
                allSkipped = false
                valuesBuilder.appendField(key, value)
            }
            if (allSkipped) return null
            return JsonObjectBuilder().appendField("values", valuesBuilder.build()).build()
        }
    }

    class SimpleBarChart(chartId: String, private val callable: Callable<Map<String, Int>>) : CustomChart(chartId) {
        override fun getChartData(): JsonObjectBuilder.JsonObject? {
            val valuesBuilder = JsonObjectBuilder()
            val map = callable.call() ?: return null
            if (map.isEmpty()) return null
            for ((key, value) in map) {
                valuesBuilder.appendField(key, intArrayOf(value))
            }
            return JsonObjectBuilder().appendField("values", valuesBuilder.build()).build()
        }
    }

    class JsonObjectBuilder {

        private var builder = StringBuilder("{")
        private var hasAtLeastOneField = false

        fun appendNull(key: String): JsonObjectBuilder {
            appendFieldUnescaped(key, "null")
            return this
        }

        fun appendField(key: String, value: String): JsonObjectBuilder {
            appendFieldUnescaped(key, "\"${escape(value)}\"")
            return this
        }

        fun appendField(key: String, value: Int): JsonObjectBuilder {
            appendFieldUnescaped(key, value.toString())
            return this
        }

        fun appendField(key: String, value: JsonObject): JsonObjectBuilder {
            appendFieldUnescaped(key, value.toString())
            return this
        }

        fun appendField(key: String, values: Array<String>): JsonObjectBuilder {
            val escapedValues = values.joinToString(",") { "\"${escape(it)}\"" }
            appendFieldUnescaped(key, "[$escapedValues]")
            return this
        }

        fun appendField(key: String, values: IntArray): JsonObjectBuilder {
            val escapedValues = values.joinToString(",")
            appendFieldUnescaped(key, "[$escapedValues]")
            return this
        }

        fun appendField(key: String, values: Array<JsonObject>): JsonObjectBuilder {
            val escapedValues = values.joinToString(",") { it.toString() }
            appendFieldUnescaped(key, "[$escapedValues]")
            return this
        }

        private fun appendFieldUnescaped(key: String?, escapedValue: String) {
            requireNotNull(key) { "JSON key must not be null" }
            if (hasAtLeastOneField) builder.append(",")
            builder.append("\"").append(escape(key)).append("\":").append(escapedValue)
            hasAtLeastOneField = true
        }

        fun build(): JsonObject {
            val obj = JsonObject(builder.append("}").toString())
            builder = StringBuilder()
            return obj
        }

        private fun escape(value: String): String {
            val sb = StringBuilder()
            for (c in value) {
                when {
                    c == '"' -> sb.append("\\\"")
                    c == '\\' -> sb.append("\\\\")
                    c <= '\u000F' -> sb.append("\\u000").append(Integer.toHexString(c.code))
                    c <= '\u001F' -> sb.append("\\u00").append(Integer.toHexString(c.code))
                    else -> sb.append(c)
                }
            }
            return sb.toString()
        }

        class JsonObject(private val value: String) {
            override fun toString(): String = value
        }
    }
}
