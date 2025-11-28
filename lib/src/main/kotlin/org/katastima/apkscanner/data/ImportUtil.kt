/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.katastima.apkscanner.config.ApkScannerConfig
import org.katastima.apkscanner.data.manifest.ManifestRepository
import org.katastima.apkscanner.database.dao.certificate.SigningCertificateDenylistEntity
import org.katastima.apkscanner.database.dao.library.LibraryEntry
import org.katastima.apkscanner.database.dao.library.LibraryInformationEntry
import org.katastima.apkscanner.database.dao.library.LibraryInformationTable
import org.katastima.apkscanner.models.library.LegacyLibraryDefinition
import org.katastima.apkscanner.models.library.LegacyLibraryInformation
import org.katastima.apkscanner.models.manifest.config.ManifestConfig
import org.katastima.apkscanner.models.signing.SigningCertificate
import org.slf4j.LoggerFactory
import kotlin.system.measureTimeMillis

object ImportUtil {

    private val LOGGER = LoggerFactory.getLogger(ImportUtil::class.java)

    suspend fun importAll(
        database: Database,
        manifestRepository: ManifestRepository,
        apkScannerConfig: ApkScannerConfig,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) = withContext(ioDispatcher) {
        listOf(
            async { importCertificateData(database, apkScannerConfig) },
            async { importLibraryData(database, apkScannerConfig) },
            async { importManifestConfigData(manifestRepository, apkScannerConfig) },
        ).awaitAll()
    }

    fun importCertificateData(database: Database, apkScannerConfig: ApkScannerConfig) {
        val json = Json { ignoreUnknownKeys = true }

        importCertificateDenylist(json, database, apkScannerConfig)
    }

    private fun importCertificateDenylist(json: Json, database: Database, apkScannerConfig: ApkScannerConfig) {
        val certificateConfigContent = DataUtil.getCertificateConfigContent(apkScannerConfig.dataConfig)
        if (certificateConfigContent.isBlank()) {
            LOGGER.warn("There is no certificate data to import, skipping import")
            return
        }

        val denyList: MutableList<SigningCertificate> = mutableListOf()
        val importDuration = measureTimeMillis {
            val jsonElement = json.parseToJsonElement(certificateConfigContent)
            jsonElement.jsonArray.forEach { denyList.add(json.decodeFromJsonElement<SigningCertificate>(it)) }

            transaction(database) {
                if (apkScannerConfig.databaseConfig.debug) {
                    addLogger(StdOutSqlLogger)
                }

                denyList.sortedBy { it.name }.forEach {
                    SigningCertificateDenylistEntity.new {
                        name = it.name
                        description = it.description
                        sourceUrl = it.sourceUrl
                        dn = it.dn.sorted()
                        sha256 = it.sha256.sorted()
                        sha1 = it.sha1.sorted()
                        md5 = it.md5.sorted()
                    }
                }
            }
        }
        LOGGER.info("Took {} ms to import {} denied certificates", importDuration, denyList.size)
    }

    fun importLibraryData(database: Database, apkScannerConfig: ApkScannerConfig) {
        val libraryInformationContent = DataUtil.getLibraryInformationContent(apkScannerConfig.dataConfig)
        if (libraryInformationContent.isEmpty()) {
            LOGGER.warn("There is no library information data to import, skipping import")
            return
        }

        val libraryDefinitionContent = DataUtil.getLibraryDefinitionContent(apkScannerConfig.dataConfig)
        if (libraryDefinitionContent.isEmpty()) {
            LOGGER.warn("There is no library definition data to import, skipping import")
            return
        }

        val json = Json { ignoreUnknownKeys = true }

        // Important: information needs to be imported before definitions
        importLibraryInformation(json, database, libraryInformationContent, apkScannerConfig.databaseConfig.debug)
        importLibraryDefinitions(json, database, libraryDefinitionContent, apkScannerConfig.databaseConfig.debug)
    }

    private fun importLibraryInformation(json: Json, database: Database, contentLines: Set<String>, debugDatabase: Boolean) {
        val libraryInformation: MutableList<LegacyLibraryInformation> = mutableListOf()

        val importDuration = measureTimeMillis {
            contentLines.forEach {
                libraryInformation.add(json.decodeFromString<LegacyLibraryInformation>(it))
            }

            transaction(database) {
                if (debugDatabase) {
                    addLogger(StdOutSqlLogger)
                }

                libraryInformation.sortedBy { it.id }.forEach {
                    LibraryInformationEntry.new {
                        libraryId = it.id
                        name = ""
                        details = it.details
                        type = ""
                        permissions = emptyList()
                        url = ""
                        modWarningId = it.modWarningId
                        antiFeatures = it.antiFeatures.asList()
                        license = it.license
                        emphasize = it.emphasize
                    }
                }
            }
        }
        LOGGER.info("Took {} ms to import {} library information entries", importDuration, libraryInformation.size)
    }

    private fun importLibraryDefinitions(json: Json, database: Database, contentLines: Set<String>, debugDatabase: Boolean) {
        val libraryDefinitions: MutableList<LegacyLibraryDefinition> = mutableListOf()

        val importDuration = measureTimeMillis {
            contentLines.forEach {
                libraryDefinitions.add(json.decodeFromString<LegacyLibraryDefinition>(it))
            }

            transaction(database) {
                if (debugDatabase) {
                    addLogger(StdOutSqlLogger)
                }

                libraryDefinitions
                    .sortedBy { it.id }
                    .forEach {
                        LibraryInformationEntry
                            .find { LibraryInformationTable.libraryId eq it.id }
                            .forEach { foundDefinition ->
                                val permissionSet = hashSetOf<String>()
                                permissionSet.addAll(foundDefinition.permissions)
                                permissionSet.addAll(it.perms)

                                foundDefinition.apply {
                                    name = it.name
                                    type = it.type
                                    permissions = permissionSet.sorted()
                                    url = it.url
                                }

                                LibraryEntry.new {
                                    path = it.path
                                    libraryInformationEntry = foundDefinition
                                }
                            }
                    }
            }
        }
        LOGGER.info("Took {} ms to import {} library definitions", importDuration, libraryDefinitions.size)
    }

    suspend fun importManifestConfigData(manifestRepository: ManifestRepository, apkScannerConfig: ApkScannerConfig): ManifestConfig {
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

            manifestRepository.importManifestConfig(manifestConfig)
        }
        LOGGER.info("Took {} ms to import manifest config {}", importDuration, manifestConfig.getGroupAndCountString())

        return manifestConfig
    }
}
