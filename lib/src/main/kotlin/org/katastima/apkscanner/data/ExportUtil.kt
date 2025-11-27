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
import org.katastima.apkscanner.config.ApkScannerConfig
import org.katastima.apkscanner.data.certificate.CertificateRepository
import org.katastima.apkscanner.data.library.LibraryRepository
import org.katastima.apkscanner.data.manifest.ManifestRepository
import org.katastima.apkscanner.internal.RepositoryUtil
import org.katastima.apkscanner.models.library.LegacyLibraryDefinition
import org.katastima.apkscanner.models.library.LegacyLibraryInformation
import org.katastima.apkscanner.models.manifest.config.ManifestConfig
import org.katastima.apkscanner.models.signing.SigningCertificate
import java.io.File

typealias ExportResultCertificateData = Pair<String, List<SigningCertificate>>
typealias ExportResultLibraryData = Triple<String, List<LegacyLibraryInformation>, List<LegacyLibraryDefinition>>
typealias ExportResultManifestData = Pair<String, ManifestConfig>

object ExportUtil {

    suspend fun exportAll(
        apkScannerConfig: ApkScannerConfig,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ): String = withContext(ioDispatcher) {
        val exportMessageBuilder = StringBuilder()

        listOf(
            async {
                val exportResult = exportCertificateDenylist(
                    RepositoryUtil.getCertificateRepository(apkScannerConfig),
                    apkScannerConfig.dataConfig.certificateDenylistExportPath,
                )
                exportMessageBuilder.append(exportResult.first).append("\n")
            },
            async {
                val exportResult = exportLibraryData(
                    RepositoryUtil.getLibraryRepository(apkScannerConfig),
                    apkScannerConfig.dataConfig.libraryDefinitionExportPath,
                    apkScannerConfig.dataConfig.libraryInformationExportPath,
                )
                exportMessageBuilder.append(exportResult.first).append("\n")
            },
            async {
                val exportResult = exportManifestConfig(
                    RepositoryUtil.getManifestRepository(apkScannerConfig),
                    apkScannerConfig.dataConfig.manifestConfigExportPath,
                )
                exportMessageBuilder.append(exportResult.first).append("\n")
            },
        ).awaitAll()

        return@withContext exportMessageBuilder.toString().trim()
    }

    suspend fun exportCertificateDenylist(
        certificateRepository: CertificateRepository,
        exportFilePath: String,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ): ExportResultCertificateData = withContext(ioDispatcher) {
        val databaseDenyList = certificateRepository.getAll()
        // Sort the deny entry list by name, ignoring case.
        val denylist: MutableList<SigningCertificate> = databaseDenyList.sortedBy { it.name.lowercase() }.toMutableList()

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
        val exportMessage = "Exported ${denylist.size} denied signing certificates to: ${denylistPath.absolutePath}"

        return@withContext ExportResultCertificateData(exportMessage, denylist)
    }

    suspend fun exportLibraryData(
        libraryRepository: LibraryRepository,
        definitionExportFilePath: String,
        informationExportFilePath: String,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ): ExportResultLibraryData = withContext(ioDispatcher) {
        val exportResultStringBuilder = StringBuilder()

        val libraryDefinitionsFile = getExportFileAndCreateParentDirectory(definitionExportFilePath)
        val legacyDefinitionList = libraryRepository.getAllDefinitionEntries()
        libraryDefinitionsFile.outputStream().bufferedWriter().use { bufferedWriter ->
            val json = Json { encodeDefaults = true }
            legacyDefinitionList.forEach { entry ->
                bufferedWriter.write(json.encodeToString(entry))
                bufferedWriter.newLine()
            }
        }
        exportResultStringBuilder.append("Exported ${legacyDefinitionList.size} library definitions to: ${libraryDefinitionsFile.absolutePath}")
        exportResultStringBuilder.append("\n")

        val legacyInformationList = libraryRepository.getAllInformationEntries()
        val libraryInformationFile = getExportFileAndCreateParentDirectory(informationExportFilePath)
        libraryInformationFile.outputStream().bufferedWriter().use { bufferedWriter ->
            val json = Json { encodeDefaults = true }
            legacyInformationList.forEach { entry ->
                bufferedWriter.write(json.encodeToString(entry))
                bufferedWriter.newLine()
            }
        }
        exportResultStringBuilder.append("Exported ${legacyInformationList.size} library information entries to: ${libraryInformationFile.absolutePath}")

        return@withContext ExportResultLibraryData(
            exportResultStringBuilder.toString(),
            legacyInformationList,
            legacyDefinitionList,
        )
    }

    suspend fun exportManifestConfig(
        manifestRepository: ManifestRepository,
        exportFilePath: String,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ): ExportResultManifestData = withContext(ioDispatcher) {
        val manifestConfig = manifestRepository.getManifestConfig()

        val exportFile = getExportFileAndCreateParentDirectory(exportFilePath)
        exportFile.outputStream().bufferedWriter().use { bufferedWriter ->
            val json = Json {
                encodeDefaults = true
                prettyPrint = true
            }
            bufferedWriter.write(json.encodeToString(manifestConfig))
        }
        val exportMessage = "Exported manifest config (${manifestConfig.getGroupAndCountString()}) to: ${exportFile.absolutePath}"

        return@withContext ExportResultManifestData(exportMessage, manifestConfig)
    }

    private fun getExportFileAndCreateParentDirectory(filePath: String): File {
        val exportFile = File(filePath)
        if (exportFile.parentFile.exists().not()) {
            exportFile.parentFile.mkdirs()
        }
        return exportFile
    }
}
