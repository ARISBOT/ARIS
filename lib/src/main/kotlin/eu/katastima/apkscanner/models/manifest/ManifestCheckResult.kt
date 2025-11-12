/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.models.manifest

import kotlinx.serialization.Serializable

@Serializable
data class ManifestCheckResult(
    val manifest: Manifest = Manifest.INVALID,
    val dangerousFlags: Set<String> = emptySet(),
    val dangerousFilters: Set<String> = emptySet(),
    val dangerousPermissions: Set<String> = emptySet(),
)
