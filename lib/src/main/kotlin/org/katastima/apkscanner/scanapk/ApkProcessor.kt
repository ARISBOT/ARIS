/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.scanapk

import brut.androlib.ApkDecoder
import brut.androlib.Config
import brut.androlib.meta.ApkInfo
import brut.directory.ExtFile
import org.katastima.apkscanner.config.ApkScannerConfig
import org.katastima.apkscanner.database.DatabaseUtil
import org.katastima.apkscanner.extensions.toSha256
import org.katastima.apkscanner.manifest.ManifestProcessor
import org.katastima.apkscanner.models.LibraryInformation
import org.katastima.apkscanner.signing.ApkCert
import org.katastima.apkscanner.utils.Randomizer
import okio.Closeable
import org.jetbrains.exposed.v1.jdbc.Database
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.createTempDirectory

class ApkProcessor(
    private val apkScannerConfig: ApkScannerConfig,
    private val database: Database,
    private val workingDirectory: File = createTempDirectory().toFile()
) : Closeable {

    fun processApk(apkFile: File): ApkScanResult {
        val (decodedApkDirectory, decodedApkInfo) = decodeApk(apkFile)

        return try {
            ApkScanResult(
                apkFilePath = getApkFilePathForReport(apkFile),
                apkFileSha256 = apkFile.toSha256(),
                signingCheckResult = ApkCert(apkFile).verify(database, apkScannerConfig),
                manifestCheckResult = ManifestProcessor(apkScannerConfig, database).processManifest(decodedApkInfo, decodedApkDirectory),
                detectedLibraries = scanForLibraries(decodedApkDirectory).sortedBy { it.name.lowercase() }.toTypedArray(),
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

    private fun scanForLibraries(outputDir: File): List<LibraryInformation> {
        val database = DatabaseUtil.getDatabase()
        val databaseConfig = ApkScannerConfig.getConfig().databaseConfig

        val libraryInformationList = mutableListOf<LibraryInformation>()

        outputDir
            // Filter by directories, where the name equals "smali".
            .listFiles { it.isDirectory && it.name.lowercase() == "smali" }
            .forEach { smaliDirectory ->
                // Walk through all the directories within the smali directory
                smaliDirectory
                    .walkTopDown()
                    .filter { it.isDirectory }
                    .forEach { directory ->
                        if (directory != smaliDirectory) {
                            var libraryId = directory.absolutePath.replace("${smaliDirectory.absolutePath}${File.separator}", "")
                            libraryId = "${File.separator}${libraryId}"

                            val libraryInformation = DatabaseUtil.getLibraryInformationFromLibraryPath(database, databaseConfig, libraryId)
                            libraryInformationList.addAll(libraryInformation)
                        }
                    }
            }

        return libraryInformationList
    }

    override fun close() {
        workingDirectory.deleteRecursively()
    }
}
