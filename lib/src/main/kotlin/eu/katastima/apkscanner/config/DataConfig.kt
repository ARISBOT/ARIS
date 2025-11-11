/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DataConfig(
    @SerialName("certificate_allowlist_path") val certificateAllowlistPath: String = "",
    @SerialName("certificate_denylist_path") val certificateDenylistPath: String = "",
    @SerialName("library_definition_path") val libraryDefinitionPath: String = "",
    @SerialName("library_information_path") val libraryInformationPath: String = "",
)
