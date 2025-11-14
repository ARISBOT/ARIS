/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.scanapk

import brut.androlib.ApkDecoder
import brut.androlib.Config
import brut.androlib.meta.ApkInfo
import brut.directory.ExtFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.Database
import org.katastima.apkscanner.config.ApkScannerConfig
import org.katastima.apkscanner.database.DatabaseUtil
import org.katastima.apkscanner.extensions.toSha256
import org.katastima.apkscanner.manifest.ManifestProcessor
import org.katastima.apkscanner.models.LibraryInformation
import org.katastima.apkscanner.signing.ApkCert
import org.katastima.apkscanner.utils.Randomizer
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.createTempDirectory
import kotlin.math.max
import kotlin.system.measureTimeMillis
import kotlin.time.measureTimedValue

class ApkProcessor(
    private val apkScannerConfig: ApkScannerConfig,
    private val database: Database,
    private val backgroundDispatcher: CoroutineDispatcher,
    private val ioDispatcher: CoroutineDispatcher,
    private val workingDirectory: File = createTempDirectory().toFile()
) : Closeable {

    suspend fun processApk(apkFile: File): ApkScanResult = withContext(backgroundDispatcher) {
        val (decodeApkPair, decodeTimeTaken) = measureTimedValue {
            decodeApk(apkFile)
        }
        LOGGER.debug("decodeApk(apkFile): {} ms", decodeTimeTaken.inWholeMilliseconds)

        val (decodedApkDirectory, decodedApkInfo) = decodeApkPair
        val (apkFilePath, apkFilePathDuration) = measureTimedValue {
            getApkFilePathForReport(apkFile)
        }
        LOGGER.debug("getApkFilePathForReport(apkFile): {} ms", apkFilePathDuration.inWholeMilliseconds)

        val (apkFileSha256, apkFileSha256Duration) = measureTimedValue {
            apkFile.toSha256()
        }
        LOGGER.debug("apkFileSha256: {} ms", apkFileSha256Duration.inWholeMilliseconds)

        val (signingCheckResult, signingCheckDuration) = measureTimedValue {
            val apkCert = ApkCert(apkFile)
            apkCert.verify(database, apkScannerConfig.databaseConfig.debug)
        }
        LOGGER.debug("signingCheckResult: {} ms", signingCheckDuration.inWholeMilliseconds)

        val (manifestCheckResult, manifestCheckDuration) = measureTimedValue {
            val manifestProcessor = ManifestProcessor(apkScannerConfig, database)
            manifestProcessor.processManifest(decodedApkInfo, decodedApkDirectory)
        }
        LOGGER.debug("manifestCheckResult: {} ms", manifestCheckDuration.inWholeMilliseconds)

        val (detectedLibraries, detectLibrariesDuration) = measureTimedValue {
            scanForLibraries(decodedApkDirectory).sortedBy { it.name.lowercase() }.toTypedArray()
        }
        LOGGER.debug("scanForLibraries: {} ms", detectLibrariesDuration.inWholeMilliseconds)

        return@withContext try {
            ApkScanResult(
                apkFilePath = apkFilePath,
                apkFileSha256 = apkFileSha256,
                signingCheckResult = signingCheckResult,
                manifestCheckResult = manifestCheckResult,
                detectedLibraries = detectedLibraries,
            )
        } finally {
            // Delete the directory (which contains the decoded apk output) recursively to clean up.
            decodedApkDirectory.deleteRecursively()
        }
    }

    private fun getApkFilePathForReport(apkFile: File): String {
        return when (apkScannerConfig.scanConfig.apkReportedPathType) {
            "absolute" -> apkFile.absolutePath

            "filename" -> apkFile.name

            "relative" -> {
                val currentWorkingDirectory = Paths.get("").toAbsolutePath().toFile()
                apkFile.relativeTo(currentWorkingDirectory).path
            }

            else -> apkFile.name
        }
    }

    private fun decodeApk(apkFile: File): Pair<File, ApkInfo> {
        // Decode the APK file using apktool, as we need the smali output.
        val apkDecoderFile = ExtFile(apkFile)
        val apkDecoderConfig = Config().apply {
            decodeAssets = Config.DecodeAssets.NONE
            decodeResources = Config.DecodeResources.FULL
            decodeSources = Config.DecodeSources.FULL
        }
        val apkDecoder = ApkDecoder(apkDecoderFile, apkDecoderConfig)

        // Generate a random string for the output directory, where the APK will be decoded into.
        val randomString = Randomizer.getRandomString()
        val outputDir = File("${workingDirectory.absolutePath}/${apkFile.name}_${randomString}/")
        val apkInfo = apkDecoder.decode(outputDir)

        return Pair(outputDir, apkInfo)
    }

    private suspend fun scanForLibraries(outputDir: File): List<LibraryInformation> {
        val libraryInformationList = mutableListOf<LibraryInformation>()

        var totalProcessingDuration = 0L
        var totalProcessedDirectories = 0L
        outputDir
            // Filter by directories, where the name equals "smali".
            .listFiles { it.isDirectory && it.name.lowercase() == "smali" }
            .forEach { smaliDirectory ->
                var processedDirectories = 0
                val processSmaliDirectoryDuration = measureTimeMillis {
                    processSmaliDirectory(smaliDirectory) {
                        libraryInformationList.addAll(it)
                        processedDirectories++
                    }
                }
                totalProcessedDirectories += processedDirectories
                totalProcessingDuration += processSmaliDirectoryDuration
                LOGGER.debug("processSmaliDirectory(): {} ms for {} directories", processSmaliDirectoryDuration, processedDirectories)
            }
        LOGGER.debug(
            "scanForLibraries(): processed a total of {} directories in {} ms ({} ms / directory)",
            totalProcessedDirectories, totalProcessingDuration, totalProcessingDuration / max(1, totalProcessedDirectories)
        )

        return libraryInformationList
    }

    private suspend fun processSmaliDirectory(smaliDirectory: File, onProcess: (informationSet: Set<LibraryInformation>) -> Unit) = withContext(ioDispatcher) {
        val smaliDirectoryPath = "${smaliDirectory.absolutePath}${File.separator}"

        // Walk through all the directories within the smali directory
        smaliDirectory
            .walkTopDown()
            .filter { it.isDirectory }
            .filter { it.absolutePath != smaliDirectory.absolutePath }
            .toSet()
            .map { async { processSmaliChildDirectory(it.absolutePath, smaliDirectoryPath) } }
            .awaitAll()
            .forEach { onProcess(it) }
    }

    private fun processSmaliChildDirectory(absoluteDirectoryPath: String, absoluteSmaliDirectoryPath: String): Set<LibraryInformation> {
        val libraryId = absoluteDirectoryPath.replace(absoluteSmaliDirectoryPath, "")
        return DatabaseUtil.getLibraryInformationFromLibraryPath(
            database = database,
            libraryPath = "${File.separator}${libraryId}",
            debugDatabase = apkScannerConfig.databaseConfig.debug
        )
    }

    override fun close() {
        workingDirectory.deleteRecursively()
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ApkProcessor::class.java)
    }
}
