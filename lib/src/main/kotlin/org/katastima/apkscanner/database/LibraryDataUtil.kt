/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.database

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.katastima.apkscanner.config.ApkScannerConfig
import org.katastima.apkscanner.database.dao.LibraryEntry
import org.katastima.apkscanner.database.dao.LibraryInformationEntry
import org.katastima.apkscanner.database.dao.LibraryInformationTable
import org.katastima.apkscanner.database.dao.LibraryTable
import org.katastima.apkscanner.models.library.LegacyLibraryDefinition
import org.katastima.apkscanner.models.library.LegacyLibraryInformation
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.system.measureTimeMillis

object LibraryDataUtil {

    private val LOGGER = LoggerFactory.getLogger(LibraryDataUtil::class.java)

    fun importLibraryData(database: Database, apkScannerConfig: ApkScannerConfig) {
        val dataConfig = apkScannerConfig.dataConfig

        val libraryInformationFile = File(dataConfig.libraryInformationPath)
        if (!libraryInformationFile.exists()) {
            LOGGER.warn("Library information path ({}) not specified or does not exist, skipping import", libraryInformationFile.absolutePath)
            return
        }

        val libraryDefinitionFile = File(dataConfig.libraryDefinitionPath)
        if (!libraryDefinitionFile.exists()) {
            LOGGER.warn("Library definition path ({}) not specified or does not exist, skipping import", libraryDefinitionFile.absolutePath)
            return
        }

        val json = Json { ignoreUnknownKeys = true }

        // Important: information needs to be imported before definitions
        importLibraryInformation(json, database, apkScannerConfig)
        importLibraryDefinitions(json, database, apkScannerConfig)
    }

    private fun importLibraryInformation(json: Json, database: Database, apkScannerConfig: ApkScannerConfig) {
        val libraryInformationFile = File(apkScannerConfig.dataConfig.libraryInformationPath)
        val libraryInformation: MutableList<LegacyLibraryInformation> = mutableListOf()

        val importDuration = measureTimeMillis {
            libraryInformationFile.readLines().forEach {
                libraryInformation.add(json.decodeFromString<LegacyLibraryInformation>(it))
            }

            transaction(database) {
                if (apkScannerConfig.databaseConfig.debug) {
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
        LOGGER.info("Imported {} library information entries from: {} in {} ms", libraryInformation.size, libraryInformationFile.absolutePath, importDuration)
    }

    private fun importLibraryDefinitions(json: Json, database: Database, apkScannerConfig: ApkScannerConfig) {
        val libraryDefinitionsFile = File(apkScannerConfig.dataConfig.libraryDefinitionPath)
        val libraryDefinitions: MutableList<LegacyLibraryDefinition> = mutableListOf()

        val importDuration = measureTimeMillis {
            libraryDefinitionsFile.readLines().forEach {
                libraryDefinitions.add(json.decodeFromString<LegacyLibraryDefinition>(it))
            }

            transaction(database) {
                if (apkScannerConfig.databaseConfig.debug) {
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
        LOGGER.info("Imported {} library definitions from: {} in {} ms", libraryDefinitions.size, libraryDefinitionsFile.absolutePath, importDuration)
    }

    fun exportLibraryDefinitions(database: Database, apkScannerConfig: ApkScannerConfig): Pair<List<LegacyLibraryInformation>, List<LegacyLibraryDefinition>> {
        val libraryDefinitionsFile = File(apkScannerConfig.dataConfig.libraryDefinitionPath)
        val libraryInformationFile = File(apkScannerConfig.dataConfig.libraryInformationPath)

        val legacyInformationList: MutableList<LegacyLibraryInformation> = mutableListOf()
        val legacyDefinitionList: MutableList<LegacyLibraryDefinition> = mutableListOf()

        transaction(database) {
            if (apkScannerConfig.databaseConfig.debug) {
                addLogger(StdOutSqlLogger)
            }

            LibraryInformationEntry
                .all()
                .sortedBy { LibraryInformationTable.libraryId }
                .forEach { libraryInformationEntry ->
                    val legacyInformation = LegacyLibraryInformation(
                        id = libraryInformationEntry.libraryId,
                        emphasize = libraryInformationEntry.emphasize,
                        details = libraryInformationEntry.details,
                        modWarningId = libraryInformationEntry.modWarningId,
                        antiFeatures = libraryInformationEntry.antiFeatures.toTypedArray(),
                        license = libraryInformationEntry.license,
                    )
                    legacyInformationList.add(legacyInformation)

                    transaction {
                        LibraryTable
                            .selectAll()
                            .where { LibraryTable.libraryInformationEntry eq libraryInformationEntry.id }
                            .sortedBy { LibraryTable.libraryInformationEntry.name }
                            .map { LibraryEntry.wrapRow(it) }
                            .forEach { libraryEntry ->
                                val legacyDefinition = LegacyLibraryDefinition(
                                    id = libraryInformationEntry.libraryId,
                                    path = libraryEntry.path,
                                    name = libraryInformationEntry.name,
                                    type = libraryInformationEntry.type,
                                    perms = libraryInformationEntry.permissions.toTypedArray(),
                                    url = libraryInformationEntry.url,
                                )
                                legacyDefinitionList.add(legacyDefinition)
                            }
                    }
                }
        }

        File("${libraryDefinitionsFile.absolutePath}.exported").outputStream().bufferedWriter().use { bufferedWriter ->
            val json = Json { encodeDefaults = true }
            legacyDefinitionList.forEach { entry ->
                bufferedWriter.write(json.encodeToString(entry))
                bufferedWriter.newLine()
            }
        }

        File("${libraryInformationFile.absolutePath}.exported").outputStream().bufferedWriter().use { bufferedWriter ->
            val json = Json { encodeDefaults = true }
            legacyInformationList.forEach { entry ->
                bufferedWriter.write(json.encodeToString(entry))
                bufferedWriter.newLine()
            }
        }

        return Pair(legacyInformationList, legacyDefinitionList)
    }
}
