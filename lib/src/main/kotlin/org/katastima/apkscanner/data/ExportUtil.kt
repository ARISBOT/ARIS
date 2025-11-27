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
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.katastima.apkscanner.config.ApkScannerConfig
import org.katastima.apkscanner.data.library.LibraryRepository
import org.katastima.apkscanner.data.manifest.ManifestRepository
import org.katastima.apkscanner.database.dao.SigningCertificateDenylistEntity
import org.katastima.apkscanner.database.dao.SigningCertificateDenylistTable
import org.katastima.apkscanner.internal.RepositoryUtil
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
            async {
                exportCertificateDenylist(
                    database,
                    apkScannerConfig.dataConfig.certificateDenylistExportPath,
                    apkScannerConfig.databaseConfig.debug,
                )
            },
            async {
                exportLibraryData(
                    RepositoryUtil.getLibraryRepository(database, apkScannerConfig),
                    apkScannerConfig.dataConfig.libraryDefinitionExportPath,
                    apkScannerConfig.dataConfig.libraryInformationExportPath,
                )
            },
            async {
                exportManifestConfig(
                    RepositoryUtil.getManifestRepository(database, apkScannerConfig),
                    apkScannerConfig.dataConfig.manifestConfigExportPath,
                )
            },
        ).awaitAll()
    }

    fun exportCertificateDenylist(database: Database, exportFile: File, debugDatabase: Boolean = false): List<SigningCertificate> {
        return exportCertificateDenylist(database, exportFile.absolutePath, debugDatabase)
    }

    fun exportCertificateDenylist(database: Database, exportFilePath: String, debugDatabase: Boolean = false): List<SigningCertificate> {
        var denylist: MutableList<SigningCertificate> = mutableListOf()

        // Get all deny list entries and store it in the list.
        transaction(database) {
            if (debugDatabase) {
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

        val denylistPath = getExportFileAndCreateParentDirectory(exportFilePath)
        denylistPath.outputStream().bufferedWriter().use { bufferedWriter ->
            val json = Json {
                encodeDefaults = true
                prettyPrint = true
            }
            bufferedWriter.write(json.encodeToString(denylist))
        }

        return denylist
    }

    suspend fun exportLibraryData(
        libraryRepository: LibraryRepository,
        definitionExportFile: File,
        informationExportFile: File,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ): Pair<List<LegacyLibraryInformation>, List<LegacyLibraryDefinition>> {
        return exportLibraryData(
            libraryRepository,
            definitionExportFile.absolutePath,
            informationExportFile.absolutePath,
            ioDispatcher,
        )
    }

    suspend fun exportLibraryData(
        libraryRepository: LibraryRepository,
        definitionExportFilePath: String,
        informationExportFilePath: String,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ): Pair<List<LegacyLibraryInformation>, List<LegacyLibraryDefinition>> = withContext(ioDispatcher) {
        val libraryDefinitionsFile = getExportFileAndCreateParentDirectory(definitionExportFilePath)
        val legacyDefinitionList = libraryRepository.getAllDefinitionEntries()
        libraryDefinitionsFile.outputStream().bufferedWriter().use { bufferedWriter ->
            val json = Json { encodeDefaults = true }
            legacyDefinitionList.forEach { entry ->
                bufferedWriter.write(json.encodeToString(entry))
                bufferedWriter.newLine()
            }
        }

        val legacyInformationList = libraryRepository.getAllInformationEntries()
        val libraryInformationFile = getExportFileAndCreateParentDirectory(informationExportFilePath)
        libraryInformationFile.outputStream().bufferedWriter().use { bufferedWriter ->
            val json = Json { encodeDefaults = true }
            legacyInformationList.forEach { entry ->
                bufferedWriter.write(json.encodeToString(entry))
                bufferedWriter.newLine()
            }
        }

        return@withContext Pair(legacyInformationList, legacyDefinitionList)
    }

    suspend fun exportManifestConfig(
        manifestRepository: ManifestRepository,
        exportFile: File,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ): ManifestConfig {
        return exportManifestConfig(manifestRepository, exportFile.absolutePath, ioDispatcher)
    }

    suspend fun exportManifestConfig(
        manifestRepository: ManifestRepository,
        exportFilePath: String,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ): ManifestConfig = withContext(ioDispatcher) {
        val manifestConfig = manifestRepository.getManifestConfig()

        val exportFile = getExportFileAndCreateParentDirectory(exportFilePath)
        exportFile.outputStream().bufferedWriter().use { bufferedWriter ->
            val json = Json {
                encodeDefaults = true
                prettyPrint = true
            }
            bufferedWriter.write(json.encodeToString(manifestConfig))
        }

        return@withContext manifestConfig
    }

    private fun getExportFileAndCreateParentDirectory(filePath: String): File {
        val exportFile = File(filePath)
        if (exportFile.parentFile.exists().not()) {
            exportFile.parentFile.mkdirs()
        }
        return exportFile
    }
}
