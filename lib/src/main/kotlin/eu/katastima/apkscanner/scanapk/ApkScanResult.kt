/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.scanapk

import eu.katastima.apkscanner.models.LibraryInformation
import eu.katastima.apkscanner.models.signing.VerificationResult

data class ApkScanResult(
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
