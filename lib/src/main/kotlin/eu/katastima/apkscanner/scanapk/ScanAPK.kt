/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.scanapk

import brut.androlib.ApkDecoder
import brut.androlib.Config
import brut.directory.ExtFile
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

    fun scanSingle(apkFile: File) {
        println("Scanning: ${apkFile.absolutePath}")

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

        // Delete the generated output directory recursively to clean up.
        outputDir.deleteRecursively()
    }

    fun scanMulti(apkFiles: List<File>) {
        apkFiles.forEach { scanSingle(it) }
    }

    override fun close() {
        workingDirectory.deleteRecursively()
    }
}
