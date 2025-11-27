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
import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.katastima.apkscanner.config.ApkScannerConfig
import org.katastima.apkscanner.database.ManifestDataUtil.getManifestConfig
import org.katastima.apkscanner.database.dao.LibraryEntry
import org.katastima.apkscanner.database.dao.LibraryInformationEntry
import org.katastima.apkscanner.database.dao.LibraryInformationTable
import org.katastima.apkscanner.database.dao.LibraryTable
import org.katastima.apkscanner.database.dao.SigningCertificateDenylistEntity
import org.katastima.apkscanner.database.dao.SigningCertificateDenylistTable
import org.katastima.apkscanner.models.library.LegacyLibraryDefinition
import org.katastima.apkscanner.models.library.LegacyLibraryInformation
import org.katastima.apkscanner.models.manifest.config.ManifestConfig
import org.katastima.apkscanner.models.signing.SigningCertificate
import java.io.File

object ExportUtil {

    suspend fun exportAll(
        database: Database,
        apkScannerConfig: ApkScannerConfig,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) = withContext(ioDispatcher) {
        listOf(
            async { exportCertificateDenylist(database, apkScannerConfig) },
            async { exportLibraryData(database, apkScannerConfig) },
            async { exportManifestConfig(database, apkScannerConfig) },
        ).awaitAll()
    }

    fun exportCertificateDenylist(database: Database, apkScannerConfig: ApkScannerConfig): List<SigningCertificate> {
        var denylist: MutableList<SigningCertificate> = mutableListOf()

        // Get all deny list entries and store it in the list.
        transaction(database) {
            if (apkScannerConfig.databaseConfig.debug) {
                addLogger(StdOutSqlLogger)
            }

            SigningCertificateDenylistTable
                .selectAll()
                .sortedBy { SigningCertificateDenylistTable.name }
                .map { SigningCertificateDenylistEntity.wrapRow(it) }
                .forEach {
                    val signingCertificate = it.toSigningCertificate()
                    denylist.add(signingCertificate)
                }
        }
        // Sort the deny entry list by name, ignoring case.
        denylist = denylist.sortedBy { it.name.lowercase() }.toMutableList()

        // If there is a template item, move it to the bottom of the list.
        val templateItem = denylist.find { it.name == "TEMPLATE_ENTRY" }
        if (templateItem != null) {
            denylist.remove(templateItem)
            denylist.addLast(templateItem)
        }

        val denylistPath = File(apkScannerConfig.dataConfig.certificateDenylistPath)
        File("${denylistPath.absolutePath}.exported").outputStream().bufferedWriter().use { bufferedWriter ->
            val json = Json {
                encodeDefaults = true
                prettyPrint = true
            }
            bufferedWriter.write(json.encodeToString(denylist))
        }

        return denylist
    }

    fun exportLibraryData(database: Database, apkScannerConfig: ApkScannerConfig): Pair<List<LegacyLibraryInformation>, List<LegacyLibraryDefinition>> {
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
