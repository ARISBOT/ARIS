/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.models.manifest.config

import kotlinx.serialization.Serializable

@Serializable
data class ManifestConfig(
    val dangerousFlags: ManifestFlagConfig = ManifestFlagConfig(),
    val dangerousFilters: ManifestFilterConfig = ManifestFilterConfig(),
    val dangerousPermissions: ManifestPermissionConfig = ManifestPermissionConfig(),
) {

    fun getGroupAndCountString(): String {
        val numberOfFlagGroups = dangerousFlags.entries.size
        var numberOfFlags = 0
        dangerousFlags.entries.forEach { numberOfFlags += it.flags.size }
        val flagString = "Flags(groups: $numberOfFlagGroups, total flags: $numberOfFlags)"

        val numberOfFilterGroups = dangerousFilters.entries.size
        var numberOfFilters = 0
        dangerousFilters.entries.forEach { numberOfFilters += it.filters.size }
        val filterString = "Filters(groups: $numberOfFilterGroups, total filters: $numberOfFilters)"

        val numberOfPermissionGroups = dangerousPermissions.entries.size
        var numberOfPermissions = 0
        dangerousPermissions.entries.forEach { numberOfPermissions += it.permissions.size }
        val permissionString = "permissions(groups: $numberOfPermissionGroups, total permissions: $numberOfPermissions)"

        return "(${flagString}, ${filterString}, ${permissionString})"
    }
}
