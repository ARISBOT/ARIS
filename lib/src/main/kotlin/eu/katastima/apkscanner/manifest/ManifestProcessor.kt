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

        // TODO: check for dangerous flags

        return dangerousFlags
    }

    private fun checkForDangerousFilters(manifest: Manifest, manifestConfig: ManifestConfig): Set<String> {
        val dangerousFilters: MutableSet<String> = mutableSetOf()

        // TODO: check for dangerous filters

        return dangerousFilters
    }

    private fun checkForDangerousPermissions(manifest: Manifest, manifestConfig: ManifestConfig): Set<String> {
        val dangerousPermissions: MutableSet<String> = mutableSetOf()

        // TODO: check for dangerous permissions

        return dangerousPermissions
    }
}
