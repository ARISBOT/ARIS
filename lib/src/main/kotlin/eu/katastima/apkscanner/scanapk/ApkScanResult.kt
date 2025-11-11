/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.scanapk

import eu.katastima.apkscanner.extensions.nowAsLocalDate
import eu.katastima.apkscanner.models.LibraryInformation
import eu.katastima.apkscanner.models.signing.VerificationResult
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class ApkScanResult(
    val scanDateUTC: LocalDateTime = nowAsLocalDate(),
    val apkFilePath: String = "",
    val apkFileSha256: String = "",
    val verificationResult: VerificationResult = VerificationResult(),
    val detectedLibraries: Array<LibraryInformation> = emptyArray(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ApkScanResult

        if (!detectedLibraries.contentEquals(other.detectedLibraries)) return false
        if (verificationResult != other.verificationResult) return false

        return true
    }

    override fun hashCode(): Int {
        var result = detectedLibraries.contentHashCode()
        result = 31 * result + verificationResult.hashCode()
        return result
    }
}
