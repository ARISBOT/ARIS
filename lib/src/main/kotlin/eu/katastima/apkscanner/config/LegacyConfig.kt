/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LegacyConfig(
    @SerialName("library_definition_path") val libraryDefinitionPath: String = "",
    @SerialName("library_information_path") val libraryInformationPath: String = "",
)
