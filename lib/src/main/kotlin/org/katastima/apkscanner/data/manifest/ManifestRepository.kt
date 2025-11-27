/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.data.manifest

import org.katastima.apkscanner.models.manifest.config.ManifestConfig
import org.katastima.apkscanner.models.manifest.config.ManifestFilterConfigEntry
import org.katastima.apkscanner.models.manifest.config.ManifestFlagConfigEntry
import org.katastima.apkscanner.models.manifest.config.ManifestPermissionConfigEntry

interface ManifestRepository {
    suspend fun getManifestConfig(): ManifestConfig

    suspend fun getAllFlagGroups(): List<ManifestFlagConfigEntry>
    suspend fun countAllFlagGroups(): Long
    suspend fun countAllFlags(): Long

    suspend fun getAllIntentFilterGroups(): List<ManifestFilterConfigEntry>
    suspend fun countAllIntentFilterGroups(): Long
    suspend fun countAllIntentFilters(): Long

    suspend fun getAllPermissionGroups(): List<ManifestPermissionConfigEntry>
    suspend fun countAllPermissionGroups(): Long
    suspend fun countAllPermissions(): Long
}
