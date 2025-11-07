/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.config

import kotlinx.serialization.Serializable

@Serializable
data class DatabaseConfig(
    val type: DatabaseType = DatabaseType.SQLITE,
    val mode: DatabaseMode = DatabaseMode.DEFAULT,
    val path: String = "apkscanner.db"
)

@Serializable
enum class DatabaseType {
    H2,
    SQLITE,
}

@Serializable
enum class DatabaseMode {
    DEFAULT,
    MEMORY,
}
