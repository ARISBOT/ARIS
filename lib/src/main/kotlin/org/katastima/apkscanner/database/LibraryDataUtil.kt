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
import java.io.File

object LibraryDataUtil {

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
