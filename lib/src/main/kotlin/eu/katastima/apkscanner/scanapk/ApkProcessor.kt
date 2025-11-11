/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.scanapk

import brut.androlib.ApkDecoder
import brut.androlib.Config
import brut.directory.ExtFile
import eu.katastima.apkscanner.config.ApkScannerConfig
import eu.katastima.apkscanner.database.DatabaseUtil
import eu.katastima.apkscanner.extensions.toSha256
import eu.katastima.apkscanner.models.LibraryInformation
import eu.katastima.apkscanner.signing.ApkCert
import eu.katastima.apkscanner.utils.Randomizer
import okio.Closeable
import org.jetbrains.exposed.v1.jdbc.Database
import java.io.File
import kotlin.io.path.createTempDirectory

class ApkProcessor(
    private val apkScannerConfig: ApkScannerConfig,
    private val database: Database,
    private val workingDirectory: File = createTempDirectory().toFile()
) : Closeable {

    fun processApk(apkFile: File): ApkScanResult {
        val decodedApkDirectory = decodeApk(apkFile)

        return try {
            ApkScanResult(
                apkFilePath = apkFile.absolutePath,
                apkFileSha256 = apkFile.toSha256(),
                verificationResult = ApkCert(apkFile).verify(database, apkScannerConfig),
                detectedLibraries = scanForLibraries(decodedApkDirectory).sortedBy { it.name.lowercase() }.toTypedArray(),
            )
        } finally {
            // Delete the directory (which contains the decoded apk output) recursively to clean up.
            decodedApkDirectory.deleteRecursively()
        }
    }

    private fun decodeApk(apkFile: File): File {
        // Decode the APK file using apktool, as we need the smali output.
        val apkDecoderFile = ExtFile(apkFile)
        val apkDecoderConfig = Config().apply {
            decodeAssets = Config.DecodeAssets.NONE
            decodeResources = Config.DecodeResources.NONE
            decodeSources = Config.DecodeSources.FULL
        }
        val apkDecoder = ApkDecoder(apkDecoderFile, apkDecoderConfig)

        // Generate a random string for the output directory, where the APK will be decoded into.
        val randomString = Randomizer.getRandomString()
        val outputDir = File("${workingDirectory.absolutePath}/${apkFile.name}_${randomString}/")
        apkDecoder.decode(outputDir)

        return outputDir
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
