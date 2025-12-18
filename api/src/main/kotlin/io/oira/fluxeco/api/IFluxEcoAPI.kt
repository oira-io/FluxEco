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

package io.oira.fluxeco.api

import io.oira.fluxeco.api.economy.IEconomyManager
import io.oira.fluxeco.api.transaction.ITransactionManager

interface IFluxEcoAPI {

    fun getEconomyManager(): IEconomyManager

    fun getTransactionManager(): ITransactionManager

    fun getVersion(): String

    companion object {
        @Volatile
        private var instance: IFluxEcoAPI? = null

        @JvmStatic
        fun getInstance(): IFluxEcoAPI {
            return instance ?: throw IllegalStateException("FluxEco API is not initialized yet")
        }

        @JvmStatic
        fun setInstance(api: IFluxEcoAPI) {
            if (instance != null) {
                throw IllegalStateException("FluxEco API instance already set")
            }
            instance = api
        }

        @JvmStatic
        fun unsetInstance() {
            instance = null
        }
    }
}

