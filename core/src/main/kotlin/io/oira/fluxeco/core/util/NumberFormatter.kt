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

package io.oira.fluxeco.core.util

import io.oira.fluxeco.core.manager.ConfigManager
import java.math.BigDecimal
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

object NumberFormatter {

    private lateinit var configManager: ConfigManager

    private var mode = "abbreviated" // "abbreviated" or "commas"
    private var maxDecimals = 2
    private var uppercase = true

    private val SUFFIXES_LOWER_DEFAULT = arrayOf("", "k", "m", "b", "t", "qa", "qt", "sx", "sp", "oc", "no", "de", "ud", "dd", "td", "qd")
    private val SUFFIXES_UPPER_DEFAULT = arrayOf("", "K", "M", "B", "T", "Qa", "Qt", "Sx", "Sp", "Oc", "No", "De", "Ud", "Dd", "Td", "Qd")
    private var POWERS = Array(16) { BigDecimal(1000).pow(it) }

    private var suffixesLower: Array<String> = SUFFIXES_LOWER_DEFAULT
    private var suffixesUpper: Array<String> = SUFFIXES_UPPER_DEFAULT

    fun init(manager: ConfigManager) {
        configManager = manager
        reload()
    }

    fun reload() {
        val config = configManager.getConfig()

        mode = config.getString("format.mode")?.lowercase() ?: "abbreviated"
        maxDecimals = config.getInt("format.max-decimal-places", 2)
        uppercase = config.getBoolean("format.uppercase", true)

        val override = config.getBoolean("format.override-abbreviations", false)
        if (override) {
            val suffixList = config.getStringList("format.suffixes")
            suffixesUpper = suffixList.toTypedArray()
            suffixesLower = suffixList.map { it.lowercase() }.toTypedArray()
        } else {
            suffixesLower = SUFFIXES_LOWER_DEFAULT
            suffixesUpper = SUFFIXES_UPPER_DEFAULT
        }

        POWERS = Array(suffixesLower.size) { BigDecimal(1000).pow(it) }
    }

    fun format(num: Double): String {
        if (num < 0) return "-${format(-num)}"

        return when (mode) {
            "commas" -> formatWithCommas(num)
            "abbreviated" -> formatAbbreviated(num)
            else -> formatAbbreviated(num) // fallback
        }
    }

    private fun formatWithCommas(num: Double): String {
        val df = DecimalFormat().apply {
            maximumFractionDigits = maxDecimals
            minimumFractionDigits = 0
            isGroupingUsed = true
        }
        return df.format(num)
    }

    private fun formatAbbreviated(num: Double): String {
        if (num < 1000) {
            val df = DecimalFormat().apply {
                maximumFractionDigits = maxDecimals
                minimumFractionDigits = 0
                isGroupingUsed = true
            }
            return df.format(num)
        }

        val index = (log10(num) / 3).toInt().coerceAtMost(suffixesLower.size - 1)
        val scaled = num / 1000.0.pow(index.toDouble())
        val suffixes = if (uppercase) suffixesUpper else suffixesLower

        val formatted = if (scaled == scaled.toInt().toDouble()) {
            scaled.toInt().toString()
        } else {
            val df = DecimalFormat().apply {
                maximumFractionDigits = maxDecimals
                minimumFractionDigits = 0
                isGroupingUsed = false
            }
            df.format(scaled)
        }

        return formatted + suffixes[index]
    }

    fun parse(input: String): Double {
        require(input.isNotBlank()) { "Input is empty" }

        val sign = if (input.startsWith("-")) -1 else 1
        val cleaned = input.replace("-", "").trim()

        val numericPart = cleaned.filter { it.isDigit() || it == '.' }
        val suffixPart = cleaned.filter { it.isLetter() }.lowercase()

        require(numericPart.isNotEmpty()) { "No numeric value found" }

        val value = numericPart.toDoubleOrNull() ?: throw NumberFormatException("Invalid number: $input")

        for (i in suffixesLower.indices.reversed()) {
            if (suffixPart == suffixesLower[i] || suffixPart == suffixesUpper[i].lowercase()) {
                return BigDecimal.valueOf(value).multiply(POWERS[i]).toDouble() * sign
            }
        }

        return value * sign
    }
}

fun Number.format(): String = NumberFormatter.format(this.toDouble())
fun String.parseNum(): Double = NumberFormatter.parse(this)
