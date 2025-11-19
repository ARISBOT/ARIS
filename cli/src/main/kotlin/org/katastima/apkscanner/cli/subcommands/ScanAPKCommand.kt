/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.cli.subcommands

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
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.serialization.json.Json
import org.katastima.apkscanner.cli.ApkScannerCommand
import org.katastima.apkscanner.database.DatabaseUtil
import org.katastima.apkscanner.extensions.formatAsHex
import org.katastima.apkscanner.extensions.formatVerifiedUnverified
import org.katastima.apkscanner.extensions.formatYesNo
import org.katastima.apkscanner.extensions.getApkFilePathForReport
import org.katastima.apkscanner.extensions.nowAsLocalDate
import org.katastima.apkscanner.models.ApkScanResult
import org.katastima.apkscanner.models.manifest.ManifestCheckResult
import org.katastima.apkscanner.models.signing.SigningBlockResult
import org.katastima.apkscanner.scanapk.ApkScanner
import org.katastima.apkscanner.signing.AndroidSigningBlock
import java.io.File


class ScanAPKCommand : ApkScannerCommand() {

    private val apkFiles: List<File> by argument("apk")
        .file(mustExist = true, mustBeReadable = true, canBeDir = false)
        .help("A single or multiple APK files which should get scanned")
        .multiple(true)

    private val storeAsJson: Int by option("--json", "-j")
        .choice(Pair("no", 0), Pair("yes", 1), Pair("pretty", 2))
        .default(0, "no")
        .help("Store the scan result as json file")

    private val jsonExcludeDefaults: Boolean by option("--json-exclude-defaults")
        .flag(default = false, defaultForHelp = "disabled")
        .help("Exclude default values when storing the scan result as json file. While this may result in smaller json files, the resulting json files may be interpreted differently by consumers.")

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
        val apkScannerConfig = cliConfig.apkScannerConfig
        val databaseConfig = apkScannerConfig.databaseConfig
        val database = DatabaseUtil.getDatabase(databaseConfig)

        ApkScanner(
            apkScannerConfig = apkScannerConfig,
            database = database,
            backgroundDispatcher = Dispatchers.Default,
            ioDispatcher = Dispatchers.IO,
        ).use {
            silenceableEcho("Scanning ${apkFiles.size} APK(s):")
            apkFiles.forEach { apkFile ->
                silenceableEcho("* ${apkFile.getApkFilePathForReport(apkScannerConfig)}")
            }
            silenceableEcho()

            it.scanMulti(apkFiles, this::printScanResult)
        }
    }

    private fun printScanResult(apkFile: File, scanResult: ApkScanResult) {
        storeScanResultAsJsonIfWanted(apkFile, scanResult)

        silenceableEcho("------------------------------------------------------------------------------")
        silenceableEcho()

        verboseEcho(message = "Scan has completed:")
        verboseEcho(message = "-------------------")
        verboseEcho(message = "* Date (UTC): ${scanResult.scanDateUTC}")
        verboseEcho(message = "* Duration: ${scanResult.scanDurationMs} ms")
        verboseEcho()

        silenceableEcho("Scanned APK:")
        silenceableEcho("------------")

        silenceableEcho("* App name: ${scanResult.manifestCheckResult.manifest.label}")
        silenceableEcho("* App id:   ${scanResult.manifestCheckResult.manifest.appId}")
        verboseEcho(EchoType.APK_INFO, "* File:     ${scanResult.apkFilePath}")
        verboseEcho(EchoType.APK_INFO, "* SHA-256:  ${scanResult.apkFileSha256}")
        silenceableEcho()

        printManifestResult(scanResult.manifestCheckResult)
        printLibraryResult(scanResult)
        printSignatureVerificationResult(scanResult)
        printAndroidSigningBlockResult(scanResult.signingCheckResult.signingBlockResult)
    }

    private fun printManifestResult(manifestCheckResult: ManifestCheckResult) {
        silenceableEcho("Manifest verification:")
        silenceableEcho("----------------------")

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
            "No violations detected."
        } else {
            manifestResultStringBuilder.toString()
        }.trim()
        silenceableEcho(manifestResultString)

        silenceableEcho()
    }

    private fun printLibraryResult(scanResult: ApkScanResult) {
        silenceableEcho("Libraries detected:")
        silenceableEcho("-------------------")
        val detectedLibraries = scanResult.libraryCheckResult.detectedLibraries
        if (detectedLibraries.isEmpty()) {
            silenceableEcho("No libraries detected.")
        } else {
            detectedLibraries.forEach { library ->
                verboseEcho(
                    EchoType.DETECTED_LIBRARIES,
                    "* ${library.name} (${library.libraryId}): ${library.type}, ${library.license}",
                    trailingNewline = false,
                )

                if (library.antiFeatures.isNotEmpty()) {
                    verboseEcho(EchoType.DETECTED_LIBRARIES, "; ${formatAntiFeatures(library.antiFeatures)}", trailingNewline = false)
                }
                verboseEcho(EchoType.DETECTED_LIBRARIES)
            }
            verboseEcho(EchoType.DETECTED_LIBRARIES)

            silenceableEcho("${detectedLibraries.size} ${if (detectedLibraries.size == 1) "library" else "libraries"} found.")
        }
        silenceableEcho()

        silenceableEcho("Offending libraries:")
        silenceableEcho("--------------------")
        val offendingLibraries = scanResult.libraryCheckResult.offendingLibraries
        if (offendingLibraries.isEmpty()) {
            silenceableEcho("No offending libraries detected.")
        } else {
            offendingLibraries.forEach { offendingLibrary ->
                silenceableEcho("* ${offendingLibrary.name} (${offendingLibrary.libraryId}): ${formatAntiFeatures(offendingLibrary.antiFeatures)}")
            }
            silenceableEcho()

            silenceableEcho("${offendingLibraries.size} offending ${if (offendingLibraries.size == 1) "library" else "libraries"} found.")
        }
        silenceableEcho()
    }

    private fun printSignatureVerificationResult(scanResult: ApkScanResult) {
        silenceableEcho("Signature verification:")
        silenceableEcho("-----------------------")

        val signingCheckResult = scanResult.signingCheckResult
        if (signingCheckResult.isInvalid()) {
            silenceableEcho("Failed to verify signature, please ensure the APK is properly signed!")
            silenceableEcho()
            return
        }

        // Print whether signature versions v1, v2 or v3 are valid.
        // v1: https://source.android.com/docs/security/features/apksigning#v1
        // v2: https://source.android.com/docs/security/features/apksigning/v2
        // v3: https://source.android.com/docs/security/features/apksigning/v3
        // v3.1: https://source.android.com/docs/security/features/apksigning/v3-1
        // v4: https://source.android.com/docs/security/features/apksigning/v4
        val apkSigResult = signingCheckResult.apkSigResult
        silenceableEcho("* apksig")
        silenceableEcho("  * Verified by apksig: ${apkSigResult.verifiedByApkSig.formatVerifiedUnverified()}")
        verboseEcho(EchoType.SIGNATURE_APKSIG, "  * Source Stamp: ${apkSigResult.sourceStampVerified.formatVerifiedUnverified()}")
        verboseEcho(EchoType.SIGNATURE_APKSIG, "  * v1: ${apkSigResult.v1.formatVerifiedUnverified()}")
        verboseEcho(EchoType.SIGNATURE_APKSIG, "  * v2: ${apkSigResult.v2.formatVerifiedUnverified()}")
        verboseEcho(EchoType.SIGNATURE_APKSIG, "  * v3: ${apkSigResult.v3.formatVerifiedUnverified()}")
        verboseEcho(EchoType.SIGNATURE_APKSIG, "  * v3.1: ${apkSigResult.v31.formatVerifiedUnverified()}")
        verboseEcho(EchoType.SIGNATURE_APKSIG, "  * v4: ${apkSigResult.v4.formatVerifiedUnverified()}")

        silenceableEcho("* Number of certificates: ${signingCheckResult.certificates.size}")
        var certificateCounter = 1
        signingCheckResult.certificateResults.forEach { certificateResult ->
            silenceableEcho("* Certificate #${certificateCounter}")

            if (certificateResult.denylistMatches.isNotEmpty()) {
                silenceableEcho("  * [!] Certificate found in deny list")
                certificateResult.denylistMatches.forEach { denyListMatch ->
                    silenceableEcho("    * Name: ${denyListMatch.name}")
                    silenceableEcho("      * Description: ${denyListMatch.description.ifEmpty { "-" }}")
                    silenceableEcho("      * Source URL:  ${denyListMatch.sourceUrl.ifEmpty { "-" }}")
                    silenceableEcho("      * DN:          ${denyListMatch.dn.ifEmpty { "-" }}")
                    silenceableEcho("      * SHA-256:     ${denyListMatch.sha256.ifEmpty { "-" }}")
                    silenceableEcho("      * SHA-1:       ${denyListMatch.sha1.ifEmpty { "-" }}")
                    silenceableEcho("      * MD5:         ${denyListMatch.md5.ifEmpty { "-" }}")
                }
            }

            silenceableEcho("  * Key Algorithm Name: ${certificateResult.sigAlgorithmName}")
            silenceableEcho("  * Key Algorithm OID:  ${certificateResult.sigAlgorithmOID}")
            silenceableEcho("  * Issuer:")
            silenceableEcho("    * Principal: ${certificateResult.issuerPrincipal}")
            silenceableEcho("    * Contains Control Characters: ${certificateResult.issuerContainsControlCharacters.formatYesNo()}")
            silenceableEcho("  * Subject:")
            silenceableEcho("    * Principal: ${certificateResult.subjectPrincipal}")
            silenceableEcho("    * Contains Control Characters: ${certificateResult.subjectContainsControlCharacters.formatYesNo()}")
            silenceableEcho("  * Not Before: ${certificateResult.notBefore}")
            silenceableEcho("  * Not After:  ${certificateResult.notAfter}")
            silenceableEcho("  * SHA-256: ${certificateResult.sha256}")
            silenceableEcho("  * SHA-1:   ${certificateResult.sha1}")
            silenceableEcho("  * MD5:     ${certificateResult.md5}")
            silenceableEcho("  * Public Key")
            silenceableEcho("    * Key Algorithm: ${certificateResult.publicKeyResult.keyAlgorithm}")
            silenceableEcho("    * Key Size (bits): ${certificateResult.publicKeyResult.keySizeBits}")
            silenceableEcho("    * SHA-256: ${certificateResult.publicKeyResult.sha256}")
            silenceableEcho("    * SHA-1:   ${certificateResult.publicKeyResult.sha1}")
            silenceableEcho("    * MD5:     ${certificateResult.publicKeyResult.md5}")
            certificateCounter++
        }

        silenceableEcho()
    }

    private fun printAndroidSigningBlockResult(signingBlockResult: SigningBlockResult) {
        silenceableEcho("Android Signing Block verification:")
        silenceableEcho("-----------------------------------")

        mapOf(
            "OK" to AndroidSigningBlock.getOkBlocks(),
            "Google" to AndroidSigningBlock.getGoogleBlocks(),
            "Payload" to AndroidSigningBlock.getPayloadBlocks(),
        ).forEach {
            val signingBlockMessage = formatSigningBlockGroup(signingBlockResult.blocks, it.key, it.value).trim()
            silenceableEcho(signingBlockMessage)
        }

        val unknownBlocks = signingBlockResult.unknownBlocksFormatted
        silenceableEcho("* Unknown blocks:")
        if (unknownBlocks.isEmpty()) {
            silenceableEcho("  * No unknown blocks")
        } else {
            unknownBlocks.forEach { silenceableEcho("  * $it") }
        }

        silenceableEcho()
    }

    private fun formatSigningBlockGroup(blocks: Set<Int>, blockType: String, blockMap: Map<Int, String>): String = buildString {
        append("* $blockType blocks:\n")
        blockMap.forEach {
            val hasBlock = blocks.contains(it.key)
            append("  * Has \"${it.value}\" block (${it.key.formatAsHex()}): ${hasBlock.formatYesNo()}\n")
        }
    }

    private fun formatAntiFeatures(antiFeatures: Array<String>): String = buildString {
        if (cliConfig.consoleOutputConfig.richOutputEnabled) {
            append("\u001B[1m")
        }
        val antiFeatureIterator = antiFeatures.iterator()
        while (antiFeatureIterator.hasNext()) {
            append(antiFeatureIterator.next())
            if (antiFeatureIterator.hasNext()) {
                append(",")
            }
        }
        if (cliConfig.consoleOutputConfig.richOutputEnabled) {
            append("\u001B[0m")
        }
    }

    private fun storeScanResultAsJsonIfWanted(apkFile: File, scanResult: ApkScanResult) {
        val scanResultJsonString = when (storeAsJson) {
            1 -> {
                @Suppress("JSON_FORMAT_REDUNDANT")
                Json { encodeDefaults = jsonExcludeDefaults.not() }.encodeToString(scanResult)
            }

            2 -> {
                @Suppress("JSON_FORMAT_REDUNDANT")
                Json { encodeDefaults = jsonExcludeDefaults.not(); prettyPrint = true }.encodeToString(scanResult)
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

    private enum class EchoType {
        GENERIC,
        APK_INFO,
        DETECTED_LIBRARIES,
        SIGNATURE_APKSIG,
    }

    private fun verboseEcho(echoType: EchoType = EchoType.GENERIC, message: Any? = "", trailingNewline: Boolean = true, err: Boolean = false) {
        val verbose = cliConfig.scanApkConfig.verboseAll || when (echoType) {
            EchoType.GENERIC -> cliConfig.scanApkConfig.verboseGeneric
            EchoType.APK_INFO -> cliConfig.scanApkConfig.verboseApkInfo
            EchoType.DETECTED_LIBRARIES -> cliConfig.scanApkConfig.verboseDetectedLibraries
            EchoType.SIGNATURE_APKSIG -> cliConfig.scanApkConfig.verboseSignatureApksig
        }
        if (cliConfig.verbose || verbose) {
            silenceableEcho(message, trailingNewline, err)
        }
    }
}
