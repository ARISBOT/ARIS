/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.models.library

import kotlinx.serialization.Serializable

@Serializable
data class LibraryCheckResult(
    val detectedLibraries: List<LibraryInformation> = emptyList(),
)
