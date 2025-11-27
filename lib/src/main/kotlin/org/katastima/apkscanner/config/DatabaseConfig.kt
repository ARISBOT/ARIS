/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DatabaseConfig(
    val debug: Boolean = true,
    val type: DatabaseType = DatabaseType.NONE,
    val mode: DatabaseMode = DatabaseMode.DEFAULT,
    val path: String = "apkscanner",
)

@Serializable
enum class DatabaseType {
    @SerialName("none")
    NONE,

    @SerialName("h2")
    H2,
}

@Serializable
enum class DatabaseMode {
    @SerialName("default")
    DEFAULT,

    @SerialName("memory")
    MEMORY,
}
