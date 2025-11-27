/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DataConfig(
    @SerialName("use_default_data") val useDefaultData: Boolean = true,
    @SerialName("certificate_denylist_path") val certificateDenylistPath: String = "",
    @SerialName("certificate_denylist_export_path") val certificateDenylistExportPath: String = "export/certificate_denylist.json",
    @SerialName("library_definition_path") val libraryDefinitionPath: String = "",
    @SerialName("library_definition_export_path") val libraryDefinitionExportPath: String = "export/libsmali.jsonl",
    @SerialName("library_information_path") val libraryInformationPath: String = "",
    @SerialName("library_information_export_path") val libraryInformationExportPath: String = "export/libinfo.jsonl",
    @SerialName("manifest_config_path") val manifestConfigPath: String = "",
    @SerialName("manifest_config_export_path") val manifestConfigExportPath: String = "export/manifest_config.json",
)
