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
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.Database
import org.katastima.apkscanner.config.ApkScannerConfig
import org.katastima.apkscanner.data.certificate.CertificateRepository
import org.katastima.apkscanner.data.library.LibraryRepository
import org.katastima.apkscanner.extensions.getApkFilePathForReport
import org.katastima.apkscanner.extensions.toSha256
import org.katastima.apkscanner.library.LibraryProcessor
import org.katastima.apkscanner.manifest.ManifestProcessor
import org.katastima.apkscanner.models.ApkScanResult
import org.katastima.apkscanner.signing.ApkCert
import org.katastima.apkscanner.utils.Randomizer
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.time.measureTimedValue

class ApkProcessor(
    private val apkScannerConfig: ApkScannerConfig,
    private val database: Database,
    private val certificateRepository: CertificateRepository,
    private val libraryRepository: LibraryRepository,
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
            apkFile.getApkFilePathForReport(apkScannerConfig)
        }
        LOGGER.debug("getApkFilePathForReport(apkFile): {} ms", apkFilePathDuration.inWholeMilliseconds)

        val (apkFileSha256, apkFileSha256Duration) = measureTimedValue {
            apkFile.toSha256()
        }
        LOGGER.debug("apkFileSha256: {} ms", apkFileSha256Duration.inWholeMilliseconds)

        val (signingCheckResult, signingCheckDuration) = measureTimedValue {
            val apkCert = ApkCert(apkFile)
            apkCert.verify(certificateRepository)
        }
        LOGGER.debug("signingCheckResult: {} ms", signingCheckDuration.inWholeMilliseconds)

        val (manifestCheckResult, manifestCheckDuration) = measureTimedValue {
            val manifestProcessor = ManifestProcessor(apkScannerConfig, database)
            manifestProcessor.processManifest(decodedApkInfo, decodedApkDirectory)
        }
        LOGGER.debug("manifestCheckResult: {} ms", manifestCheckDuration.inWholeMilliseconds)

        val (libraryCheckResult, detectLibrariesDuration) = measureTimedValue {
            val libraryProcessor = LibraryProcessor(libraryRepository, backgroundDispatcher, ioDispatcher)
            libraryProcessor.process(decodedApkDirectory)
        }
        LOGGER.debug("libraryCheckResult: {} ms", detectLibrariesDuration.inWholeMilliseconds)

        return@withContext try {
            ApkScanResult(
                apkFilePath = apkFilePath,
                apkFileSha256 = apkFileSha256,
                signingCheckResult = signingCheckResult,
                manifestCheckResult = manifestCheckResult,
                libraryCheckResult = libraryCheckResult,
            )
        } finally {
            // Delete the directory (which contains the decoded apk output) recursively to clean up.
            decodedApkDirectory.deleteRecursively()
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

    override fun close() {
        workingDirectory.deleteRecursively()
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ApkProcessor::class.java)
    }
}
