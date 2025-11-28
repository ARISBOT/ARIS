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
import org.katastima.apkscanner.config.ApkScannerConfig
import org.katastima.apkscanner.data.certificate.CertificateRepository
import org.katastima.apkscanner.data.library.LibraryRepository
import org.katastima.apkscanner.data.manifest.ManifestRepository
import org.katastima.apkscanner.models.library.LegacyLibraryDefinition
import org.katastima.apkscanner.models.library.LegacyLibraryInformation
import org.katastima.apkscanner.models.manifest.config.ManifestConfig
import org.katastima.apkscanner.models.signing.SigningCertificate
import org.slf4j.LoggerFactory
import kotlin.system.measureTimeMillis

object ImportUtil {

    private val LOGGER = LoggerFactory.getLogger(ImportUtil::class.java)

    suspend fun importAll(
        certificateRepository: CertificateRepository,
        libraryRepository: LibraryRepository,
        manifestRepository: ManifestRepository,
        apkScannerConfig: ApkScannerConfig,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) = withContext(ioDispatcher) {
        listOf(
            async { importCertificateData(certificateRepository, apkScannerConfig) },
            async { importLibraryData(libraryRepository, apkScannerConfig) },
            async { importManifestConfigData(manifestRepository, apkScannerConfig) },
        ).awaitAll()
    }

    suspend fun importCertificateData(certificateRepository: CertificateRepository, apkScannerConfig: ApkScannerConfig) {
        val json = Json { ignoreUnknownKeys = true }

        importCertificateDenylist(json, certificateRepository, apkScannerConfig)
    }

    private suspend fun importCertificateDenylist(json: Json, certificateRepository: CertificateRepository, apkScannerConfig: ApkScannerConfig) {
        val certificateConfigContent = DataUtil.getCertificateConfigContent(apkScannerConfig.dataConfig)
        if (certificateConfigContent.isBlank()) {
            LOGGER.warn("There is no certificate data to import, skipping import")
            return
        }

        val denyList: MutableList<SigningCertificate> = mutableListOf()
        val importDuration = measureTimeMillis {
            val jsonElement = json.parseToJsonElement(certificateConfigContent)
            val jsonArray = jsonElement.jsonArray
            jsonArray.forEach { denyList.add(json.decodeFromJsonElement<SigningCertificate>(it)) }

            certificateRepository.importCertificates(denyList)
        }
        LOGGER.info("Took {} ms to import {} denied certificates", importDuration, denyList.size)
    }

    suspend fun importLibraryData(libraryRepository: LibraryRepository, apkScannerConfig: ApkScannerConfig) {
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
        importLibraryInformation(json, libraryRepository, libraryInformationContent)
        importLibraryDefinitions(json, libraryRepository, libraryDefinitionContent)
    }

    private suspend fun importLibraryInformation(json: Json, libraryRepository: LibraryRepository, contentLines: Set<String>) {
        val libraryInformation: MutableList<LegacyLibraryInformation> = mutableListOf()

        val importDuration = measureTimeMillis {
            contentLines.forEach {
                libraryInformation.add(json.decodeFromString<LegacyLibraryInformation>(it))
            }

            libraryRepository.importInformationEntries(libraryInformation)
        }
        LOGGER.info("Took {} ms to import {} library information entries", importDuration, libraryInformation.size)
    }

    private suspend fun importLibraryDefinitions(json: Json, libraryRepository: LibraryRepository, contentLines: Set<String>) {
        val libraryDefinitions: MutableList<LegacyLibraryDefinition> = mutableListOf()

        val importDuration = measureTimeMillis {
            contentLines.forEach {
                libraryDefinitions.add(json.decodeFromString<LegacyLibraryDefinition>(it))
            }

            libraryRepository.importDefinitionEntries(libraryDefinitions)
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
