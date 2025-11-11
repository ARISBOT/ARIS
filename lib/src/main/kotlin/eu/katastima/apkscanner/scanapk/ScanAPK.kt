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
import java.io.Closeable
import java.io.File
import kotlin.io.path.createTempDirectory

class ScanAPK : Closeable {

    private val workingDirectory: File

    /**
     * Explicitly declared constructor to provide compatibility with the current way how things are working.
     * Library definitions are split into two files:
     * - libsmali.jsonl (used fields: id, name, type, url)
     * - libinfo.jsonl (used fields: id, details, anti)
     *
     * These files are getting loaded and checked against the smali output to detect libraries.
     *
     * Other projects are also making use of these files, so we cannot get rid of them right now.
     */
    constructor(workingDirectory: File? = null) {
        this.workingDirectory = workingDirectory ?: createTempDirectory().toFile()
    }

    fun scanSingle(apkFile: File): ApkScanResult {
        val decodedApkDirectory = decodeApk(apkFile)

        val database = DatabaseUtil.getDatabase()
        val apkScannerConfig = ApkScannerConfig.getConfig()

        val apkScanResult = ApkScanResult(
            apkFilePath = apkFile.absolutePath,
            apkFileSha256 = apkFile.toSha256(),
            verificationResult = ApkCert(apkFile).verify(database, apkScannerConfig),
            detectedLibraries = scanForLibraries(decodedApkDirectory).sortedBy { it.name.lowercase() }.toTypedArray(),
        )

        // Delete the directory (which contains the decoded apk output) recursively to clean up.
        decodedApkDirectory.deleteRecursively()

        return apkScanResult
    }

    fun scanMulti(apkFiles: List<File>, scanCallback: ((apkFile: File, scanResult: ApkScanResult) -> Unit)? = null): Map<File, ApkScanResult> {
        val apkScanResultMap: MutableMap<File, ApkScanResult> = mutableMapOf()
        apkFiles.forEach { apkFile ->
            val scanResult = scanSingle(apkFile)
            apkScanResultMap[apkFile] = scanResult

            // If callback is specified, invoke it.
            scanCallback?.invoke(apkFile, scanResult)
        }
        return apkScanResultMap
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
