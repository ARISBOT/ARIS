/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.database

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.katastima.apkscanner.config.ApkScannerConfig
import org.katastima.apkscanner.data.DataUtil
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
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.system.measureTimeMillis

object ManifestDataUtil {

    private val LOGGER = LoggerFactory.getLogger(ManifestDataUtil::class.java)

    fun importManifestConfigData(database: Database, apkScannerConfig: ApkScannerConfig): ManifestConfig {
        val json = Json { ignoreUnknownKeys = true }

        val manifestConfigContent = DataUtil.getManifestConfigContent(apkScannerConfig.dataConfig)
        if (manifestConfigContent.isBlank()) {
            LOGGER.warn("There is no manifest data to import, skipping import")
            return ManifestConfig()
        }

        val manifestConfig: ManifestConfig

        val importDuration = measureTimeMillis {
            val jsonElement = json.parseToJsonElement(manifestConfigContent)
            manifestConfig = json.decodeFromJsonElement(jsonElement)

            transaction(database) {
                if (apkScannerConfig.databaseConfig.debug) {
                    addLogger(StdOutSqlLogger)
                }

                // Flags
                manifestConfig.dangerousFlags.entries.sortedBy { it.name }.forEach {
                    ManifestFlagConfigEntity.new {
                        name = it.name
                        description = it.description
                        flags = it.flags.sorted()
                    }
                }

                // Filters
                manifestConfig.dangerousFilters.entries.sortedBy { it.name }.forEach {
                    ManifestFilterConfigEntity.new {
                        name = it.name
                        description = it.description
                        filters = it.filters.sorted()
                    }
                }

                // Permissions
                manifestConfig.dangerousPermissions.entries.sortedBy { it.name }.forEach {
                    ManifestPermissionConfigEntity.new {
                        name = it.name
                        description = it.description
                        permissions = it.permissions.sorted()
                    }
                }
            }
        }
        LOGGER.info("Imported manifest config {} in {} ms", manifestConfig.getGroupAndCountString(), importDuration)

        return manifestConfig
    }

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
