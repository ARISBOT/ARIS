/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.scanapk

import eu.katastima.apkscanner.models.LibraryInformation

data class ApkScanResult(
    val detectedLibraries: Array<LibraryInformation> = emptyArray(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ApkScanResult

        if (!detectedLibraries.contentEquals(other.detectedLibraries)) return false

        return true
    }

    override fun hashCode(): Int {
        return detectedLibraries.contentHashCode()
    }
}
