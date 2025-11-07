/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.scanapk

import brut.androlib.ApkDecoder
import brut.androlib.Config
import brut.directory.ExtFile
import eu.katastima.apkscanner.config.ApkScannerConfig
import eu.katastima.apkscanner.models.LegacyLibraryDefinition
import eu.katastima.apkscanner.models.LegacyLibraryInformation
import eu.katastima.apkscanner.utils.Randomizer
import kotlinx.serialization.json.Json
import java.io.Closeable
import java.io.File
import kotlin.io.path.createTempDirectory

class ScanAPK : Closeable {

    private val libraryDefinitionsFile: File
    private val libraryInformationFile: File
    private val workingDirectory: File

    private val libraryDefinitions: MutableList<LegacyLibraryDefinition> = mutableListOf()
    private val libraryInformation: MutableList<LegacyLibraryInformation> = mutableListOf()

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
        val legacyConfig = ApkScannerConfig.getConfig().legacyConfig
        this.libraryDefinitionsFile = File(legacyConfig.libraryDefinitionPath)
        this.libraryInformationFile = File(legacyConfig.libraryInformationPath)
        this.workingDirectory = workingDirectory ?: createTempDirectory().toFile()
    }

    fun scanSingle(apkFile: File) {
        println("Scanning: ${apkFile.absolutePath}")

        loadLibraryInformation()
        loadLibraryDefinitions()

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

        outputDir
            // Filter by directories, where the name equals "smali".
            .listFiles { it.isDirectory && it.name.lowercase() == "smali" }
            .forEach { smaliDirectory ->
                println("Processing smali directory (${smaliDirectory.absolutePath})")

                // Walk through all the directories within the smali directory
                smaliDirectory.walkTopDown()
                    .filter { it.isDirectory }
                    .forEach {
                        if (it != smaliDirectory) {
                            // TODO: compare the output with the definition list.
                            println(it.absolutePath.replace("${smaliDirectory.absolutePath}${File.separator}", ""))
                        }
                    }
            }

        // TODO: remove
        File("${libraryDefinitionsFile.absolutePath}.new").outputStream().bufferedWriter().use { bufferedWriter ->
            val json = Json { encodeDefaults = true }
            libraryDefinitions.forEach { entry ->
                bufferedWriter.write(json.encodeToString(entry))
                bufferedWriter.newLine()
            }
        }

        // TODO: remove
        File("${libraryInformationFile.absolutePath}.new").outputStream().bufferedWriter().use { bufferedWriter ->
            val json = Json { encodeDefaults = true }
            libraryInformation.forEach { entry ->
                bufferedWriter.write(json.encodeToString(entry))
                bufferedWriter.newLine()
            }
        }

        // Delete the generated output directory recursively to clean up.
        outputDir.deleteRecursively()
    }

    fun scanMulti(apkFiles: List<File>) {
        apkFiles.forEach { scanSingle(it) }
    }

    override fun close() {
        workingDirectory.deleteRecursively()
    }

    private fun loadLibraryDefinitions() {
        if (libraryDefinitions.isEmpty()) {
            // TODO: ensure the jsonl is fixed upstream.
            val json = Json { ignoreUnknownKeys = true }
            libraryDefinitionsFile.readLines().forEach {
                libraryDefinitions.add(json.decodeFromString<LegacyLibraryDefinition>(it))
            }

            println("Loaded ${libraryDefinitions.size} library definitions")
            if (libraryDefinitions.isNotEmpty()) {
                println("First library definition: ${libraryDefinitions.first()}")
            }
        }
    }

    private fun loadLibraryInformation() {
        if (libraryInformation.isEmpty()) {
            // TODO: ensure the jsonl is fixed upstream.
            val json = Json { ignoreUnknownKeys = true }
            libraryInformationFile.readLines().forEach {
                libraryInformation.add(json.decodeFromString<LegacyLibraryInformation>(it))
            }

            println("Loaded ${libraryInformation.size} library information entries")
            if (libraryInformation.isNotEmpty()) {
                println("First library information entry: ${libraryInformation.first()}")
            }
        }
    }
}
