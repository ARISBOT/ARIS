/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.utils

import eu.katastima.apkscanner.config.ApkScannerConfig
import eu.katastima.apkscanner.database.dao.LibraryEntry
import eu.katastima.apkscanner.database.dao.LibraryInformationEntry
import eu.katastima.apkscanner.database.dao.LibraryInformationTable
import eu.katastima.apkscanner.database.dao.LibraryTable
import eu.katastima.apkscanner.models.LegacyLibraryDefinition
import eu.katastima.apkscanner.models.LegacyLibraryInformation
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.File

object LegacyUtil {

    fun importLegacyData(database: Database, apkScannerConfig: ApkScannerConfig) {
        // TODO: ensure the jsonl is fixed upstream.
        val json = Json { ignoreUnknownKeys = true }

        val libraryDefinitionsFile = File(apkScannerConfig.legacyConfig.libraryDefinitionPath)
        val libraryDefinitions: MutableList<LegacyLibraryDefinition> = mutableListOf()
        libraryDefinitionsFile.readLines().forEach {
            libraryDefinitions.add(json.decodeFromString<LegacyLibraryDefinition>(it))
        }
        println("Imported ${libraryDefinitions.size} library definitions from: ${libraryDefinitionsFile.absolutePath}")

        val libraryInformationFile = File(apkScannerConfig.legacyConfig.libraryInformationPath)
        val libraryInformation: MutableList<LegacyLibraryInformation> = mutableListOf()
        libraryInformationFile.readLines().forEach {
            libraryInformation.add(json.decodeFromString<LegacyLibraryInformation>(it))
        }
        println("Imported ${libraryInformation.size} library information entries from: ${libraryInformationFile.absolutePath}")

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

            libraryDefinitions.sortedBy { it.id }.forEach {
                LibraryInformationEntry.find { LibraryInformationTable.libraryId eq it.id }.forEach { foundDefinition ->
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
                        libraryId = it.id
                        //libraryDefinition = foundDefinition
                    }
                }
            }
        }
    }

    fun exportLibraryDefinitions(database: Database, apkScannerConfig: ApkScannerConfig): Pair<List<LegacyLibraryInformation>, List<LegacyLibraryDefinition>> {
        val libraryDefinitionsFile = File(apkScannerConfig.legacyConfig.libraryDefinitionPath)
        val libraryInformationFile = File(apkScannerConfig.legacyConfig.libraryInformationPath)

        val legacyInformationList: MutableList<LegacyLibraryInformation> = mutableListOf()
        val legacyDefinitionList: MutableList<LegacyLibraryDefinition> = mutableListOf()

        transaction(database) {
            if (apkScannerConfig.databaseConfig.debug) {
                addLogger(StdOutSqlLogger)
            }

            LibraryInformationTable
                .selectAll()
                .sortedBy { LibraryInformationTable.libraryId }
                .map { LibraryInformationEntry.wrapRow(it) }
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
                            .where { LibraryTable.libraryId eq libraryInformationEntry.libraryId }
                            .sortedBy { LibraryTable.libraryId }
                            .map { LibraryEntry.wrapRow(it) }
                            .forEach { libraryEntry ->
                                val legacyDefinition = LegacyLibraryDefinition(
                                    id = libraryEntry.libraryId,
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
