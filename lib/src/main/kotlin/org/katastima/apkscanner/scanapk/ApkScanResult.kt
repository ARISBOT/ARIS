/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.scanapk

import org.katastima.apkscanner.extensions.nowAsLocalDate
import org.katastima.apkscanner.models.LibraryInformation
import org.katastima.apkscanner.models.manifest.ManifestCheckResult
import org.katastima.apkscanner.models.signing.SigningCheckResult
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApkScanResult(
    val scanDateUTC: LocalDateTime = nowAsLocalDate(),
    val scanDurationMs: Long = 0,
    val apkFilePath: String = "",
    val apkFileSha256: String = "",
    @SerialName("signingCheckResults") val signingCheckResult: SigningCheckResult = SigningCheckResult(),
    val manifestCheckResult: ManifestCheckResult = ManifestCheckResult(),
    val detectedLibraries: Array<LibraryInformation> = emptyArray(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ApkScanResult

        if (!detectedLibraries.contentEquals(other.detectedLibraries)) return false
        if (signingCheckResult != other.signingCheckResult) return false

        return true
    }

    override fun hashCode(): Int {
        var result = detectedLibraries.contentHashCode()
        result = 31 * result + signingCheckResult.hashCode()
        return result
    }
}
