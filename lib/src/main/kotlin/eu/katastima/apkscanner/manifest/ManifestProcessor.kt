/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.manifest

import brut.androlib.meta.ApkInfo
import eu.katastima.apkscanner.config.ApkScannerConfig
import eu.katastima.apkscanner.database.ManifestDataUtil
import eu.katastima.apkscanner.models.manifest.Manifest
import eu.katastima.apkscanner.models.manifest.ManifestCheckResult
import eu.katastima.apkscanner.models.manifest.config.ManifestConfig
import org.jetbrains.exposed.v1.jdbc.Database
import java.io.File

class ManifestProcessor(
    private val apkScannerConfig: ApkScannerConfig,
    private val database: Database,
) {

    fun processManifest(decodedApkInfo: ApkInfo, decodedApkDirectory: File): ManifestCheckResult {
        val manifestFile = File(decodedApkDirectory, "AndroidManifest.xml")

        val packageName = AndroidManifestUtil.pullPackageName(manifestFile) ?: ""
        val applicationLabel = AndroidManifestUtil.pullApplicationLabel(manifestFile) ?: ""
        val features = AndroidManifestUtil.pullFeatures(manifestFile).sortedBy { it.name }
        val flags = AndroidManifestUtil.pullApplicationFlags(manifestFile).sortedBy { it.name }
        val intentFilters = AndroidManifestUtil.pullIntentFilters(manifestFile).sortedBy { it.actions.firstOrNull()?.name ?: "" }
        val permissions = AndroidManifestUtil.pullPermissions(manifestFile).sortedBy { it.name }

        val libDir = File(decodedApkDirectory, "lib")
        val abis = if (libDir.exists()) {
            libDir.listFiles { it.isDirectory }.map { it.name }
        } else {
            emptyList()
        }

        val manifest = Manifest(
            appId = packageName,
            versionCode = decodedApkInfo.versionInfo.versionCode.toInt(),
            versionName = decodedApkInfo.versionInfo.versionName,
            minSdk = decodedApkInfo.sdkInfo.minSdkVersion.toInt(),
            targetSdk = decodedApkInfo.sdkInfo.targetSdkVersion.toInt(),
            features = features,
            flags = flags,
            intentFilters = intentFilters,
            permissions = permissions,
            abis = abis,
            label = applicationLabel,
        )
        val manifestConfig = ManifestDataUtil.getManifestConfig(database, apkScannerConfig)

        return ManifestCheckResult(
            manifest = manifest,
            dangerousFlags = checkForDangerousFlags(manifest, manifestConfig),
            dangerousFilters = checkForDangerousFilters(manifest, manifestConfig),
            dangerousPermissions = checkForDangerousPermissions(manifest, manifestConfig),
        )
    }

    private fun checkForDangerousFlags(manifest: Manifest, manifestConfig: ManifestConfig): Set<String> {
        val dangerousFlags: MutableSet<String> = mutableSetOf()

        val manifestFlags = manifest.flags.map { it.name }

        // Check each flag entry, which contains [name, description, flags].
        manifestConfig.dangerousFlags.entries.forEach { dangerousFlagEntry ->
            // Check each flag within the entry
            dangerousFlagEntry.flags.forEach { dangerousFlag ->
                // If the manifest contains the dangerous flag, add it to the set.
                if (manifestFlags.contains(dangerousFlag)) {
                    dangerousFlags.add(dangerousFlag)
                }
            }
        }

        return dangerousFlags
    }

    private fun checkForDangerousFilters(manifest: Manifest, manifestConfig: ManifestConfig): Set<String> {
        val dangerousFilters: MutableSet<String> = mutableSetOf()

        // Each intent filter can have multiple actions, so we need to flatten it first and then map its name to get a set of strings.
        // Use a set, because only unique actions need to be checked as it does not matter if a dangerous intent filter is used multiple times.
        val manifestFilterActionSet = manifest.intentFilters.flatMap { it.actions }.map { it.name }.toSet()

        // Check each filter entry, which contains [name, description, filters].
        manifestConfig.dangerousFilters.entries.forEach { dangerousFilterEntry ->
            // Check each flag within the entry
            dangerousFilterEntry.filters.forEach { dangerousFilter ->
                // If the manifest contains the dangerous filter, add it to the set.
                if (manifestFilterActionSet.contains(dangerousFilter)) {
                    dangerousFilters.add(dangerousFilter)
                }
            }
        }

        return dangerousFilters
    }

    private fun checkForDangerousPermissions(manifest: Manifest, manifestConfig: ManifestConfig): Set<String> {
        val dangerousPermissions: MutableSet<String> = mutableSetOf()

        val manifestPermissions = manifest.permissions.map { it.name }

        // Check each permission entry, which contains [name, description, permissions].
        manifestConfig.dangerousPermissions.entries.forEach { dangerousPermissionEntry ->
            // Check each permission within the entry
            dangerousPermissionEntry.permissions.forEach { dangerousPermission ->
                // If the manifest contains the dangerous flag, add it to the set.
                if (manifestPermissions.contains(dangerousPermission)) {
                    dangerousPermissions.add(dangerousPermission)
                }
            }
        }

        return dangerousPermissions
    }
}
