/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.cli.subcommands

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import org.katastima.apkscanner.extensions.formatAsHex
import org.katastima.apkscanner.extensions.formatValidInvalid
import org.katastima.apkscanner.extensions.formatYesNo
import org.katastima.apkscanner.extensions.nowAsLocalDate
import org.katastima.apkscanner.models.LibraryInformation
import org.katastima.apkscanner.models.manifest.ManifestCheckResult
import org.katastima.apkscanner.models.signing.SigningBlockResult
import org.katastima.apkscanner.scanapk.ApkScanResult
import org.katastima.apkscanner.scanapk.ScanAPK
import org.katastima.apkscanner.signing.AndroidSigningBlock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.serialization.json.Json
import java.io.File


class ScanAPKCommand : SuspendingCliktCommand() {

    private val apkFiles: List<File> by argument("apk")
        .file(mustExist = true, mustBeReadable = true, canBeDir = false)
        .help("A single or multiple APK files which should get scanned")
        .multiple(true)

    private val storeAsJson: Int by option("--json", "-j")
        .choice(Pair("no", 0), Pair("yes", 1), Pair("pretty", 2))
        .default(0, "no")
        .help("Store the scan result as json file")

    private val jsonEncodeDefaults: Boolean by option("--json-include-defaults")
        .flag(default = false, defaultForHelp = "disabled")
        .help("Encode default values as well when storing the scan result as json file")

    private val jsonResultOutputDirectory: File by option("--output", "-o")
        .file(canBeFile = false)
        .default(File("output").absoluteFile)
        .help("A directory where scan output should be stored. The directory will be created, if it does not already exist.")

    override val printHelpOnEmptyArgs = true

    private val scanStartedAt: LocalDateTime = nowAsLocalDate()
    private val localDateTimeFormatter = LocalDateTime.Format {
        date(LocalDate.Formats.ISO_BASIC)
        char('_')
        hour(); minute(); second()
        char('_')
        secondFraction(fixedLength = 3)
    }

    init {
        context {
            helpFormatter = { MordantHelpFormatter(it, showDefaultValues = true, requiredOptionMarker = "*") }
        }
    }

    override fun help(context: Context): String = "Scan a single apk and list its used libraries, offending libraries and anti features."

    override suspend fun run() {
        ScanAPK().use {
            echo("Scanning ${apkFiles.size} APK(s).\n")
            it.scanMulti(apkFiles, this::printScanResult)
        }
        echo("Have a nice day!")
    }

    private fun printScanResult(apkFile: File, scanResult: ApkScanResult) {
        storeScanResultAsJsonIfWanted(apkFile, scanResult)

        echo("Scan has completed:")
        echo("-------------------")
        echo("* Date (UTC): ${scanResult.scanDateUTC}")
        echo("* Duration: ${scanResult.scanDurationMs} ms")
        echo()

        echo("Scanned APK:")
        echo("------------")

        echo("* App name: ${scanResult.manifestCheckResult.manifest.label}")
        echo("* App id:   ${scanResult.manifestCheckResult.manifest.appId}")
        echo("* File:     ${scanResult.apkFilePath}")
        echo("* SHA-256:  ${scanResult.apkFileSha256}")
        echo()

        printManifestResult(scanResult.manifestCheckResult)
        printLibraryResult(scanResult)
        printSignatureVerificationResult(scanResult)
        printAndroidSigningBlockResult(scanResult.signingCheckResult.signingBlockResult)

        echo("------------------------------------------------------------------------------")
        echo()
    }

    private fun printManifestResult(manifestCheckResult: ManifestCheckResult) {
        echo("Manifest verification:")
        echo("----------------------")

        val manifestResultStringBuilder = StringBuilder()

        if (manifestCheckResult.dangerousFilters.isNotEmpty()) {
            manifestResultStringBuilder.append("* Dangerous filters\n")
            manifestCheckResult.dangerousFilters.forEach { manifestResultStringBuilder.append("  * $it\n") }
        }

        if (manifestCheckResult.dangerousFlags.isNotEmpty()) {
            manifestResultStringBuilder.append("* Dangerous flags\n")
            manifestCheckResult.dangerousFlags.forEach { manifestResultStringBuilder.append("  * $it\n") }
        }

        if (manifestCheckResult.dangerousPermissions.isNotEmpty()) {
            manifestResultStringBuilder.append("* Dangerous permissions\n")
            manifestCheckResult.dangerousPermissions.forEach { manifestResultStringBuilder.append("  * $it\n") }
        }

        val manifestResultString = if (manifestResultStringBuilder.isEmpty()) {
            "* No violations detected"
        } else {
            manifestResultStringBuilder.toString()
        }.trim()
        echo(manifestResultString)

        echo()
    }

    private fun printLibraryResult(scanResult: ApkScanResult) {
        // Store offending libraries in an own list to prevent having to iterate through the whole list multiple times.
        val offendingLibraries: MutableList<LibraryInformation> = mutableListOf()

        echo("Libraries detected:")
        echo("-------------------")

        if (scanResult.detectedLibraries.isEmpty()) {
            echo("* No libraries detected")
        } else {
            scanResult.detectedLibraries.forEach { library ->
                echo("* ${library.name} (${library.libraryId}): ${library.type}, ${library.license}", trailingNewline = false)

                if (library.antiFeatures.isNotEmpty()) {
                    echo("; ${formatAntiFeatures(library.antiFeatures)}", trailingNewline = false)
                    offendingLibraries.add(library)
                }
                echo()
            }
        }
        echo()

        echo("Offending libraries:")
        echo("--------------------")
        if (offendingLibraries.isEmpty()) {
            echo("* No offending libraries detected")
        } else {
            echo("Offending libraries:")
            echo("--------------------")
            offendingLibraries.forEach { offendingLibrary ->
                echo("* ${offendingLibrary.name} (${offendingLibrary.libraryId}): ${formatAntiFeatures(offendingLibrary.antiFeatures)}")
            }
            echo()

            echo("${offendingLibraries.size} offending ${if (offendingLibraries.size == 1) "library" else "libraries"} found.")
        }
        echo()
    }

    private fun printSignatureVerificationResult(scanResult: ApkScanResult) {
        echo("Signature verification:")
        echo("-----------------------")

        val signingCheckResult = scanResult.signingCheckResult
        if (signingCheckResult.isInvalid()) {
            echo("* Failed to verify signature, please ensure the APK is properly signed!")
            echo()
            return
        }

        // Print whether signature versions v1, v2 or v3 are valid.
        // v1: https://source.android.com/docs/security/features/apksigning#v1
        // v2: https://source.android.com/docs/security/features/apksigning/v2
        // v3: https://source.android.com/docs/security/features/apksigning/v3
        // v3.1: https://source.android.com/docs/security/features/apksigning/v3-1
        // v4: https://source.android.com/docs/security/features/apksigning/v4
        val apkSigResult = signingCheckResult.apkSigResult
        echo("* apksig")
        echo("  * Verified by apksig: ${apkSigResult.verifiedByApkSig.formatValidInvalid()}")
        echo("  * Source Stamp: ${apkSigResult.sourceStampVerified.formatValidInvalid()}")
        echo("  * v1: ${apkSigResult.v1.formatValidInvalid()}")
        echo("  * v2: ${apkSigResult.v2.formatValidInvalid()}")
        echo("  * v3: ${apkSigResult.v3.formatValidInvalid()}")
        echo("  * v3.1: ${apkSigResult.v31.formatValidInvalid()}")
        echo("  * v4: ${apkSigResult.v4.formatValidInvalid()}")
        echo("* Number of certificates: ${signingCheckResult.certificates.size}")

        var certificateCounter = 1
        signingCheckResult.certificateResults.forEach { certificateResult ->
            echo("* Certificate #${certificateCounter}")

            if (certificateResult.denylistMatches.isNotEmpty()) {
                echo("  * [!] Certificate found in deny list")
                certificateResult.denylistMatches.forEach { denyListMatch ->
                    echo("    * Name: ${denyListMatch.name}")
                    echo("      * Description: ${denyListMatch.description.ifEmpty { "-" }}")
                    echo("      * Source URL:  ${denyListMatch.sourceUrl.ifEmpty { "-" }}")
                    echo("      * DN:          ${denyListMatch.dn.ifEmpty { "-" }}")
                    echo("      * SHA-256:     ${denyListMatch.sha256.ifEmpty { "-" }}")
                    echo("      * SHA-1:       ${denyListMatch.sha1.ifEmpty { "-" }}")
                    echo("      * MD5:         ${denyListMatch.md5.ifEmpty { "-" }}")
                }
            }

            echo("  * Key Algorithm Name: ${certificateResult.sigAlgorithmName}")
            echo("  * Key Algorithm OID:  ${certificateResult.sigAlgorithmOID}")
            echo("  * Issuer:")
            echo("    * Principal: ${certificateResult.issuerPrincipal}")
            echo("    * Contains Control Characters: ${certificateResult.issuerContainsControlCharacters.formatYesNo()}")
            echo("  * Subject:")
            echo("    * Principal: ${certificateResult.subjectPrincipal}")
            echo("    * Contains Control Characters: ${certificateResult.subjectContainsControlCharacters.formatYesNo()}")
            echo("  * Not Before: ${certificateResult.notBefore}")
            echo("  * Not After:  ${certificateResult.notAfter}")
            echo("  * SHA-256: ${certificateResult.sha256}")
            echo("  * SHA-1:   ${certificateResult.sha1}")
            echo("  * MD5:     ${certificateResult.md5}")
            echo("  * Public Key")
            echo("    * Key Algorithm: ${certificateResult.publicKeyResult.keyAlgorithm}")
            echo("    * Key Size (bits): ${certificateResult.publicKeyResult.keySizeBits}")
            echo("    * SHA-256: ${certificateResult.publicKeyResult.sha256}")
            echo("    * SHA-1:   ${certificateResult.publicKeyResult.sha1}")
            echo("    * MD5:     ${certificateResult.publicKeyResult.md5}")
            certificateCounter++
        }

        echo()
    }

    private fun printAndroidSigningBlockResult(signingBlockResult: SigningBlockResult) {
        echo("Android Signing Block verification:")
        echo("-----------------------------------")

        mapOf(
            "OK" to AndroidSigningBlock.getOkBlocks(),
            "Google" to AndroidSigningBlock.getGoogleBlocks(),
            "Payload" to AndroidSigningBlock.getPayloadBlocks(),
        ).forEach {
            val signingBlockMessage = formatSigningBlockGroup(signingBlockResult.blocks, it.key, it.value).trim()
            echo(signingBlockMessage)
        }

        val unknownBlocks = signingBlockResult.unknownBlocksFormatted
        echo("* Unknown blocks:")
        if (unknownBlocks.isEmpty()) {
            echo("  * No unknown blocks")
        } else {
            unknownBlocks.forEach { echo("  * $it") }
        }

        echo()
    }

    private fun formatSigningBlockGroup(blocks: Set<Int>, blockType: String, blockMap: Map<Int, String>): String = buildString {
        append("* $blockType blocks:\n")
        blockMap.forEach {
            val hasBlock = blocks.contains(it.key)
            append("  * Has \"${it.value}\" block (${it.key.formatAsHex()}): ${hasBlock.formatYesNo()}\n")
        }
    }

    private fun formatAntiFeatures(antiFeatures: Array<String>): String = buildString {
        // TODO: configurable console output formatting.
        append("\u001B[1m")
        val antiFeatureIterator = antiFeatures.iterator()
        while (antiFeatureIterator.hasNext()) {
            append(antiFeatureIterator.next())
            if (antiFeatureIterator.hasNext()) {
                append(",")
            }
        }
        // TODO: configurable console output formatting.
        append("\u001B[0m")
    }

    private fun storeScanResultAsJsonIfWanted(apkFile: File, scanResult: ApkScanResult) {
        val scanResultJsonString = when (storeAsJson) {
            1 -> {
                Json { encodeDefaults = jsonEncodeDefaults }.encodeToString(scanResult)
            }

            2 -> {
                Json { encodeDefaults = jsonEncodeDefaults; prettyPrint = true }.encodeToString(scanResult)
            }

            else -> {
                // Do nothing
                ""
            }
        }
        if (scanResultJsonString.isNotEmpty()) {
            val resultOutputDirectory = File(jsonResultOutputDirectory, scanStartedAt.format(localDateTimeFormatter))
            resultOutputDirectory.mkdirs()

            val resultOutputFile = File(resultOutputDirectory, "apk-scanner_scan-apk_${apkFile.name}.json")
            resultOutputFile.writeText(scanResultJsonString)
        }
    }
}
