/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.cli.configs

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScanApkConfig(
    @SerialName("verbose_all") var verboseAll: Boolean = false,
    @SerialName("verbose_generic") var verboseGeneric: Boolean = false,
    @SerialName("verbose_apk_info") var verboseApkInfo: Boolean = true,
    @SerialName("verbose_detected_libraries") var verboseDetectedLibraries: Boolean = true,
    @SerialName("verbose_signature_apksig") var verboseSignatureApksig: Boolean = false,
    @SerialName("verbose_signature_certificate") var verboseSignatureCertificate: Boolean = false,
    @SerialName("verbose_signing_block") var verboseSigningBlock: Boolean = false,
    @SerialName("store_as_json") var storeAsJson: OutputStoreType = OutputStoreType.NO,
    @SerialName("json_exclude_defaults") var jsonExcludeDefaults: Boolean = false,
    @SerialName("json_output_directory") var jsonOutputDirectory: String = "",
    @SerialName("json_output_subdirectory") var jsonOutputSubdirectory: Boolean = false,
    @SerialName("json_output_with_apk") var jsonOutputWithApk: Boolean = false,
)

@Serializable
enum class OutputStoreType {
    @SerialName("no")
    NO,

    @SerialName("yes")
    YES,

    @SerialName("pretty")
    PRETTY,
}
