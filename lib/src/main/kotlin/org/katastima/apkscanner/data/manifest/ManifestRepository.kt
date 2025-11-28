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
    suspend fun importManifestConfig(manifestConfig: ManifestConfig): Result<Long>

    suspend fun getAllFlagGroups(): List<ManifestFlagConfigEntry>
    suspend fun countAllFlagGroups(): Long
    suspend fun countAllFlags(): Long
    suspend fun addFlagGroup(manifestFlagGroup: ManifestFlagConfigEntry): Result<Long>
    suspend fun updateFlagGroup(manifestFlagGroup: ManifestFlagConfigEntry): Result<Long>
    suspend fun deleteFlagGroup(manifestFlagGroup: ManifestFlagConfigEntry): Result<Long>

    suspend fun getAllIntentFilterGroups(): List<ManifestFilterConfigEntry>
    suspend fun countAllIntentFilterGroups(): Long
    suspend fun countAllIntentFilters(): Long
    suspend fun addIntentFilterGroup(intentFilterGroup: ManifestFilterConfigEntry): Result<Long>
    suspend fun updateIntentFilterGroup(intentFilterGroup: ManifestFilterConfigEntry): Result<Long>
    suspend fun deleteIntentFilterGroup(intentFilterGroup: ManifestFilterConfigEntry): Result<Long>

    suspend fun getAllPermissionGroups(): List<ManifestPermissionConfigEntry>
    suspend fun countAllPermissionGroups(): Long
    suspend fun countAllPermissions(): Long
    suspend fun addPermissionGroup(permissionGroup: ManifestPermissionConfigEntry): Result<Long>
    suspend fun updatePermissionGroup(permissionGroup: ManifestPermissionConfigEntry): Result<Long>
    suspend fun deletePermissionGroup(permissionGroup: ManifestPermissionConfigEntry): Result<Long>
}
