/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.database

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.katastima.apkscanner.config.ApkScannerConfig
import org.katastima.apkscanner.database.dao.ManifestFilterConfigEntity
import org.katastima.apkscanner.database.dao.ManifestFilterConfigTable
import org.katastima.apkscanner.database.dao.ManifestFlagConfigEntity
import org.katastima.apkscanner.database.dao.ManifestFlagConfigTable
import org.katastima.apkscanner.database.dao.ManifestPermissionConfigEntity
import org.katastima.apkscanner.database.dao.ManifestPermissionConfigTable
import org.katastima.apkscanner.models.manifest.config.ManifestConfig
import org.katastima.apkscanner.models.manifest.config.ManifestFilterConfig
import org.katastima.apkscanner.models.manifest.config.ManifestFlagConfig
import org.katastima.apkscanner.models.manifest.config.ManifestPermissionConfig
import java.io.File

object ManifestDataUtil {

    fun getManifestConfig(database: Database, apkScannerConfig: ApkScannerConfig): ManifestConfig {
        return transaction(database) {
            if (apkScannerConfig.databaseConfig.debug) {
                addLogger(StdOutSqlLogger)
            }

            val manifestFlags = ManifestFlagConfigTable
                .selectAll()
                .map { ManifestFlagConfigEntity.wrapRow(it).toManifestFlagConfigEntry() }
                .toSet()
            val manifestFilters = ManifestFilterConfigTable
                .selectAll()
                .map { ManifestFilterConfigEntity.wrapRow(it).toManifestFilterConfigEntry() }
                .toSet()
            val manifestPermissions = ManifestPermissionConfigTable
                .selectAll()
                .map { ManifestPermissionConfigEntity.wrapRow(it).toManifestPermissionConfigEntry() }
                .toSet()

            return@transaction ManifestConfig(
                dangerousFlags = ManifestFlagConfig(
                    entries = manifestFlags,
                ),
                dangerousFilters = ManifestFilterConfig(
                    entries = manifestFilters,
                ),
                dangerousPermissions = ManifestPermissionConfig(
                    entries = manifestPermissions,
                ),
            )
        }
    }

    fun exportManifestConfig(database: Database, apkScannerConfig: ApkScannerConfig): ManifestConfig {
        val manifestConfig = getManifestConfig(database, apkScannerConfig)

        val manifestConfigPath = File(apkScannerConfig.dataConfig.manifestConfigPath)
        File("${manifestConfigPath.absolutePath}.exported").outputStream().bufferedWriter().use { bufferedWriter ->
            val json = Json {
                encodeDefaults = true
                prettyPrint = true
            }
            bufferedWriter.write(json.encodeToString(manifestConfig))
        }

        return manifestConfig
    }
}
