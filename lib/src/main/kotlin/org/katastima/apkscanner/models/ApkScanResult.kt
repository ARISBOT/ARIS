/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.models

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.katastima.apkscanner.extensions.nowAsLocalDate
import org.katastima.apkscanner.models.library.LibraryCheckResult
import org.katastima.apkscanner.models.manifest.ManifestCheckResult
import org.katastima.apkscanner.models.signing.SigningCheckResult

@Serializable
data class ApkScanResult(
    val scanDateUTC: LocalDateTime = nowAsLocalDate(),
    val scanDurationMs: Long = 0,
    val apkFilePath: String = "",
    val apkFileSha256: String = "",
    @SerialName("signingCheckResult") val signingCheckResult: SigningCheckResult? = null,
    @SerialName("manifestCheckResult") val manifestCheckResult: ManifestCheckResult? = null,
    @SerialName("libraryCheckResult") val libraryCheckResult: LibraryCheckResult? = null,
)
