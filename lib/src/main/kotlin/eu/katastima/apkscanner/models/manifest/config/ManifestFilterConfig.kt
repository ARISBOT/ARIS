/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.models.manifest.config

import kotlinx.serialization.Serializable

@Serializable
data class ManifestFilterConfig(
    val entries: Set<ManifestFilterConfigEntry> = emptySet(),
)

@Serializable
data class ManifestFilterConfigEntry(
    val name: String = "",
    val description: String = "",
    val filters: Set<String> = emptySet(),
)
