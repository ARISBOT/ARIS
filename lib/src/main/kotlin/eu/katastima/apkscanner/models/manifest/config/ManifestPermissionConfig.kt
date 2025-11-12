/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.models.manifest.config

import kotlinx.serialization.Serializable

@Serializable
data class ManifestPermissionConfig(
    val entries: Set<ManifestPermissionConfigEntry> = emptySet(),
)

@Serializable
data class ManifestPermissionConfigEntry(
    val name: String = "",
    val description: String = "",
    val permissions: Set<String> = emptySet(),
)
