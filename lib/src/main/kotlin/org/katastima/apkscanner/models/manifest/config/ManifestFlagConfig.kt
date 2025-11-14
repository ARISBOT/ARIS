/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.models.manifest.config

import kotlinx.serialization.Serializable

@Serializable
data class ManifestFlagConfig(
    val entries: Set<ManifestFlagConfigEntry> = emptySet(),
)

@Serializable
data class ManifestFlagConfigEntry(
    val name: String = "",
    val description: String = "",
    val flags: Set<String> = emptySet(),
)
