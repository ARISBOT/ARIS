/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.cli.configs

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConsoleOutputConfig(
    @SerialName("enable_rich_output") var richOutputEnabled: Boolean = true,
)
