/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.scanapk

import brut.androlib.ApkDecoder
import brut.androlib.Config
import brut.androlib.meta.ApkInfo
import brut.directory.ExtFile
import eu.katastima.apkscanner.config.ApkScannerConfig
import eu.katastima.apkscanner.database.DatabaseUtil
import eu.katastima.apkscanner.extensions.toSha256
import eu.katastima.apkscanner.manifest.AndroidManifestUtil
import eu.katastima.apkscanner.models.LibraryInformation
import eu.katastima.apkscanner.models.manifest.Manifest
import eu.katastima.apkscanner.models.manifest.ManifestCheckResult
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
        val (decodedApkDirectory, decodedApkInfo) = decodeApk(apkFile)

        return try {
            ApkScanResult(
                apkFilePath = apkFile.absolutePath,
                apkFileSha256 = apkFile.toSha256(),
                signingCheckResult = ApkCert(apkFile).verify(database, apkScannerConfig),
                manifestCheckResult = processManifest(decodedApkInfo, decodedApkDirectory),
                detectedLibraries = scanForLibraries(decodedApkDirectory).sortedBy { it.name.lowercase() }.toTypedArray(),
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

    private fun processManifest(decodedApkInfo: ApkInfo, decodedApkDirectory: File): ManifestCheckResult {
        val manifestFile = File(decodedApkDirectory, "AndroidManifest.xml")

        val packageName = AndroidManifestUtil.pullPackageName(manifestFile) ?: ""
        val applicationLabel = AndroidManifestUtil.pullApplicationLabel(manifestFile) ?: ""
        val features = AndroidManifestUtil.pullFeatures(manifestFile).sortedBy { it.name }
        val permissions = AndroidManifestUtil.pullPermissions(manifestFile).sortedBy { it.name }

        val libDir = File(decodedApkDirectory, "lib")
        val abis = if (libDir.exists()) {
            libDir.listFiles { it.isDirectory }.map { it.name }
        } else {
            emptyList()
        }

        val manifest = Manifest(
            appId = packageName,
            versionCode = decodedApkInfo.versionInfo.versionCode.toInt(),
            versionName = decodedApkInfo.versionInfo.versionName,
            minSdk = decodedApkInfo.sdkInfo.minSdkVersion.toInt(),
            targetSdk = decodedApkInfo.sdkInfo.targetSdkVersion.toInt(),
            features = features,
            permissions = permissions,
            abis = abis,
            label = applicationLabel,
        )

        // TODO: check for bad things :O

        return ManifestCheckResult(
            manifest = manifest,
        )
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
