/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.config

import kotlinx.serialization.Serializable

@Serializable
data class DatabaseConfig(
    val debug: Boolean = true,
    val type: DatabaseType = DatabaseType.H2,
    val mode: DatabaseMode = DatabaseMode.DEFAULT,
    val path: String = "apkscanner",
)

@Serializable
enum class DatabaseType {
    H2,
}

@Serializable
enum class DatabaseMode {
    DEFAULT,
    MEMORY,
}
