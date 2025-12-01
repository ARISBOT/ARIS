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
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.nullableFlag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.char
import kotlinx.serialization.json.Json
import org.katastima.apkscanner.cli.ApkScannerCommand
import org.katastima.apkscanner.cli.configs.OutputStoreType
import org.katastima.apkscanner.cli.extensions.formatBold
import org.katastima.apkscanner.cli.extensions.formatGreen
import org.katastima.apkscanner.cli.extensions.formatRed
import org.katastima.apkscanner.cli.extensions.formatYellow
import org.katastima.apkscanner.extensions.formatAsHex
import org.katastima.apkscanner.extensions.formatVerifiedUnverified
import org.katastima.apkscanner.extensions.formatYesNo
import org.katastima.apkscanner.extensions.getApkFilePathForReport
import org.katastima.apkscanner.extensions.nowAsLocalDate
import org.katastima.apkscanner.internal.RepositoryUtil
import org.katastima.apkscanner.models.ApkScanResult
import org.katastima.apkscanner.models.manifest.Manifest
import org.katastima.apkscanner.models.manifest.ManifestCheckResult
import org.katastima.apkscanner.models.signing.ApkSigResult
import org.katastima.apkscanner.models.signing.CertificateResult
import org.katastima.apkscanner.models.signing.SigningBlockResult
import org.katastima.apkscanner.scanapk.ApkScanner
import org.katastima.apkscanner.signing.AndroidSigningBlock
import org.katastima.apkscanner.utils.AndroidApiLevels
import java.io.File


class ScanAPKCommand : ApkScannerCommand() {

    private val apkFiles: List<File> by argument("apk")
        .file(mustExist = true, mustBeReadable = true, canBeDir = false)
        .help("A single or multiple APK files which should get scanned")
        .multiple(true)

    private val storeAsJson: OutputStoreType? by option("--json", "-j")
        .choice(Pair("no", OutputStoreType.NO), Pair("yes", OutputStoreType.YES), Pair("pretty", OutputStoreType.PRETTY))
        .help("Store the scan result as json file")

    private val jsonExcludeDefaults: Boolean? by option("--json-exclude-defaults")
        .nullableFlag("--json-include-defaults")
        .help("Exclude default values when storing the scan result as json file. While this may result in smaller json files, the resulting json files may be interpreted differently by consumers.")

    private val jsonResultOutputDirectory: File? by option("--output", "-o")
        .file(canBeFile = false)
        .help("A directory where the scan output should be stored. The directory will be created, if it does not already exist.")

    private val jsonOutputSubdirectory: Boolean? by option("--output-subdirectory")
        .nullableFlag()
        .help("Store the scan output in a subdirectory within the output directory.")

    private val outputResultWithApk: Boolean? by option("--output-with-apk")
        .nullableFlag()
        .help("Store the scan output next to the APK file(s) (suffixed with '.json') instead of writing to a file within the specified output directory.")


    override val printHelpOnEmptyArgs = true

    private val scanStartedAt: LocalDateTime = nowAsLocalDate()
    private val localDateTimeFormatter: DateTimeFormat<LocalDateTime> by lazy {
        LocalDateTime.Format {
            date(LocalDate.Formats.ISO_BASIC)
            char('_')
            hour(); minute(); second()
            char('_')
            secondFraction(fixedLength = 3)
        }
    }

    init {
        context {
            helpFormatter = { MordantHelpFormatter(it, showDefaultValues = true, requiredOptionMarker = "*") }
        }
    }

    override fun help(context: Context): String = "Scan a single apk and list its used libraries, offending libraries and anti features."

    override suspend fun run() {
        val apkScannerConfig = cliConfig.apkScannerConfig

        val certificateRepository = RepositoryUtil.getCertificateRepository(apkScannerConfig)
        val libraryRepository = RepositoryUtil.getLibraryRepository(apkScannerConfig)
        val manifestRepository = RepositoryUtil.getManifestRepository(apkScannerConfig)

        ApkScanner(
            apkScannerConfig = apkScannerConfig,
            certificateRepository = certificateRepository,
            libraryRepository = libraryRepository,
            manifestRepository = manifestRepository,
            backgroundDispatcher = Dispatchers.Default,
            ioDispatcher = Dispatchers.IO,
        ).use {
            val forcePrintApkScanList = apkFiles.size > 1
            verboseEcho(EchoType.GENERIC, "Scanning ${apkFiles.size} APK(s):", forcePrint = forcePrintApkScanList)
            apkFiles.forEach { apkFile ->
                verboseEcho(EchoType.GENERIC, "* ${apkFile.getApkFilePathForReport(apkScannerConfig)}", forcePrint = forcePrintApkScanList)
            }
            verboseEcho(EchoType.GENERIC, forcePrint = forcePrintApkScanList)

            it.scanMulti(apkFiles, this::printScanResult)
        }
    }

    private fun printScanResult(apkFile: File, scanResult: ApkScanResult) {
        // Add separator when scanning multiple apk files.
        if (apkFiles.size > 1) {
            silenceableEcho("------------------------------------------------------------------------------")
            silenceableEcho()
        }

        verboseEcho(message = "Scan has completed:")
        verboseEcho(message = "-------------------")
        verboseEcho(message = "* Date (UTC): ${scanResult.scanDateUTC}")
        verboseEcho(message = "* Duration: ${scanResult.scanDurationMs} ms")
        verboseEcho()

        storeScanResultAsJsonIfWanted(apkFile, scanResult)

        silenceableEcho("Scanned APK:")
        silenceableEcho("------------")

        val apkManifest = scanResult.manifestCheckResult.manifest
        silenceableEcho("* Name:    ${apkManifest.label}")
        silenceableEcho("* Package: ${apkManifest.appId}")
        silenceableEcho("* Version: ${apkManifest.versionName} (${apkManifest.versionCode})")

        silenceableEcho("* SDK:")
        val minSdkAndroidVersion = AndroidApiLevels.getAndroidVersionForApiLevel(apkManifest.minSdk)
        val minSdkAndroidVersionCode = AndroidApiLevels.getAndroidVersionCodeForApiLevel(apkManifest.minSdk)
        val minSdkAndroidCodename = AndroidApiLevels.getAndroidCodenameForApiLevel(apkManifest.minSdk)
        silenceableEcho("  * MinSDK:    ${apkManifest.minSdk} ($minSdkAndroidVersion - $minSdkAndroidVersionCode - $minSdkAndroidCodename)")
        val targetSdkAndroidVersion = AndroidApiLevels.getAndroidVersionForApiLevel(apkManifest.targetSdk)
        val targetSdkAndroidVersionCode = AndroidApiLevels.getAndroidVersionCodeForApiLevel(apkManifest.targetSdk)
        val targetSdkAndroidCodename = AndroidApiLevels.getAndroidCodenameForApiLevel(apkManifest.targetSdk)
        silenceableEcho("  * TargetSDK: ${apkManifest.targetSdk} ($targetSdkAndroidVersion - $targetSdkAndroidVersionCode - $targetSdkAndroidCodename)")

        // TODO: compiler information (platformBuildVersion, compileSdkVersion) as verbose?

        verboseEcho(EchoType.APK_INFO, "* File:    ${scanResult.apkFilePath}")
        verboseEcho(EchoType.APK_INFO, "* SHA-256: ${scanResult.apkFileSha256}")
        silenceableEcho()

        printManifestResult(scanResult.manifestCheckResult)
        printLibraryResult(scanResult)
        printSignatureVerificationResult(scanResult)
        printAndroidSigningBlockResult(scanResult.signingCheckResult.signingBlockResult)
    }

    private fun printManifestResult(manifestCheckResult: ManifestCheckResult) {
        silenceableEcho("Manifest verification:")
        silenceableEcho("----------------------")

        if (manifestCheckResult.dangerousFilters.isNotEmpty()) {
            silenceableEcho("* Dangerous filters".formatYellow(cliConfig.consoleOutputConfig))
            manifestCheckResult.dangerousFilters.forEach { silenceableEcho("  * $it".formatYellow(cliConfig.consoleOutputConfig)) }
        }

        if (manifestCheckResult.dangerousFlags.isNotEmpty()) {
            silenceableEcho("* Dangerous flags".formatYellow(cliConfig.consoleOutputConfig))
            manifestCheckResult.dangerousFlags.forEach { silenceableEcho("  * $it".formatYellow(cliConfig.consoleOutputConfig)) }
        }

        if (manifestCheckResult.dangerousPermissions.isNotEmpty()) {
            silenceableEcho("* Dangerous permissions".formatYellow(cliConfig.consoleOutputConfig))
            manifestCheckResult.dangerousPermissions.forEach { silenceableEcho("  * $it".formatYellow(cliConfig.consoleOutputConfig)) }
        }

        if (
            manifestCheckResult.dangerousFilters.isEmpty() &&
            manifestCheckResult.dangerousFlags.isEmpty() &&
            manifestCheckResult.dangerousPermissions.isEmpty()
        ) {
            silenceableEcho("No offenders detected.".formatGreen(cliConfig.consoleOutputConfig))
        }
        silenceableEcho()

        printPermissions(manifestCheckResult.manifest)
    }

    private fun printPermissions(manifest: Manifest) {
        verboseEcho(EchoType.PERMISSIONS, "Permissions:")
        verboseEcho(EchoType.PERMISSIONS, "------------")

        if (manifest.permissions.isEmpty()) {
            verboseEcho(EchoType.PERMISSIONS, "No permissions detected.")
        } else {
            manifest.permissions.forEach { permission ->
                val sdkMessage = if (permission.minSdk > 0 || permission.maxSdk > 0) {
                    "(min: ${permission.minSdk}, max: ${permission.maxSdk})"
                } else {
                    ""
                }
                verboseEcho(EchoType.PERMISSIONS, "* ${permission.name} $sdkMessage".trim())
            }
            verboseEcho(EchoType.PERMISSIONS)

            verboseEcho(EchoType.PERMISSIONS, "Found ${manifest.permissions.size} permissions.")
        }
        verboseEcho(EchoType.PERMISSIONS)
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
            silenceableEcho("No offending libraries detected.".formatGreen(cliConfig.consoleOutputConfig))
        } else {
            offendingLibraries.forEach { offendingLibrary ->
                silenceableEcho("* ${offendingLibrary.name} (${offendingLibrary.libraryId}): ${formatAntiFeatures(offendingLibrary.antiFeatures)}")
            }
            silenceableEcho()

            val offendingLibrariesFoundMessage = "${offendingLibraries.size} offending ${if (offendingLibraries.size == 1) "library" else "libraries"} found."
            silenceableEcho(offendingLibrariesFoundMessage.formatYellow(cliConfig.consoleOutputConfig))
        }
        silenceableEcho()
    }

    private fun printSignatureVerificationResult(scanResult: ApkScanResult) {
        silenceableEcho("Signature verification:")
        silenceableEcho("-----------------------")

        val signingCheckResult = scanResult.signingCheckResult
        if (signingCheckResult.isInvalid()) {
            silenceableEcho("Failed to verify signature, please ensure the APK is properly signed!".formatRed(cliConfig.consoleOutputConfig))
            silenceableEcho()
            return
        }

        printSignatureApksig(signingCheckResult.apkSigResult)

        silenceableEcho("* Number of certificates: ${signingCheckResult.certificates.size}")
        var certificateCounter = 1
        signingCheckResult.certificateResults.forEach { certificateResult ->
            silenceableEcho("* Certificate #${certificateCounter}")

            if (certificateResult.denylistMatches.isNotEmpty()) {
                silenceableEcho("  * [!] Certificate found in deny list".formatRed(cliConfig.consoleOutputConfig))
                certificateResult.denylistMatches.forEach { denyListMatch ->
                    silenceableEcho("    * Name: ${denyListMatch.name}")
                    silenceableEcho("      * Description: ${denyListMatch.description.ifEmpty { "-" }}")
                    silenceableEcho("      * Source URL:  ${denyListMatch.sourceUrl.ifEmpty { "-" }}")
                    verboseEcho(EchoType.SIGNATURE_CERTIFICATE, "      * DN:          ${denyListMatch.dn.ifEmpty { "-" }}")
                    verboseEcho(EchoType.SIGNATURE_CERTIFICATE, "      * SHA-256:     ${denyListMatch.sha256.ifEmpty { "-" }}")
                    verboseEcho(EchoType.SIGNATURE_CERTIFICATE, "      * SHA-1:       ${denyListMatch.sha1.ifEmpty { "-" }}")
                    verboseEcho(EchoType.SIGNATURE_CERTIFICATE, "      * MD5:         ${denyListMatch.md5.ifEmpty { "-" }}")
                }
            }

            silenceableEcho("  * Key Algorithm Name: ${certificateResult.sigAlgorithmName}")
            verboseEcho(EchoType.SIGNATURE_CERTIFICATE, "  * Key Algorithm OID:  ${certificateResult.sigAlgorithmOID}")

            printSignaturePrincipals(certificateResult)

            silenceableEcho("  * Not Before: ${certificateResult.notBefore}")
            silenceableEcho("  * Not After:  ${certificateResult.notAfter}")
            silenceableEcho("  * SHA-256: ${certificateResult.sha256}")
            verboseEcho(EchoType.SIGNATURE_CERTIFICATE, "  * SHA-1:   ${certificateResult.sha1}")
            verboseEcho(EchoType.SIGNATURE_CERTIFICATE, "  * MD5:     ${certificateResult.md5}")
            silenceableEcho("  * Public Key")
            silenceableEcho("    * Key Algorithm: ${certificateResult.publicKeyResult.keyAlgorithm}")
            silenceableEcho("    * Key Size (bits): ${certificateResult.publicKeyResult.keySizeBits}")
            verboseEcho(EchoType.SIGNATURE_CERTIFICATE, "    * SHA-256: ${certificateResult.publicKeyResult.sha256}")
            verboseEcho(EchoType.SIGNATURE_CERTIFICATE, "    * SHA-1:   ${certificateResult.publicKeyResult.sha1}")
            verboseEcho(EchoType.SIGNATURE_CERTIFICATE, "    * MD5:     ${certificateResult.publicKeyResult.md5}")
            certificateCounter++
        }

        silenceableEcho()
    }

    private fun printSignatureApksig(apkSigResult: ApkSigResult) {
        // Print whether signature versions v1, v2 or v3 are valid.
        // v1: https://source.android.com/docs/security/features/apksigning#v1
        // v2: https://source.android.com/docs/security/features/apksigning/v2
        // v3: https://source.android.com/docs/security/features/apksigning/v3
        // v3.1: https://source.android.com/docs/security/features/apksigning/v3-1
        // v4: https://source.android.com/docs/security/features/apksigning/v4
        silenceableEcho("* apksig")
        silenceableEcho("  * Verified by apksig: ${apkSigResult.verifiedByApkSig.formatVerifiedUnverified()}")
        verboseEcho(EchoType.SIGNATURE_APKSIG, "  * Source Stamp: ${apkSigResult.sourceStampVerified.formatVerifiedUnverified()}")
        verboseEcho(EchoType.SIGNATURE_APKSIG, "  * v1: ${apkSigResult.v1.formatVerifiedUnverified()}")
        verboseEcho(EchoType.SIGNATURE_APKSIG, "  * v2: ${apkSigResult.v2.formatVerifiedUnverified()}")
        verboseEcho(EchoType.SIGNATURE_APKSIG, "  * v3: ${apkSigResult.v3.formatVerifiedUnverified()}")
        verboseEcho(EchoType.SIGNATURE_APKSIG, "  * v3.1: ${apkSigResult.v31.formatVerifiedUnverified()}")
        verboseEcho(EchoType.SIGNATURE_APKSIG, "  * v4: ${apkSigResult.v4.formatVerifiedUnverified()}")
    }

    private fun printSignaturePrincipals(certificateResult: CertificateResult) {
        val issuerMatchesSubject = certificateResult.issuerPrincipal == certificateResult.subjectPrincipal
        if (!issuerMatchesSubject || certificateResult.issuerContainsControlCharacters) {
            if (!issuerMatchesSubject) {
                silenceableEcho("  * Issuer does NOT match subject")
            }
            silenceableEcho("  * Issuer:")
            silenceableEcho("    * ${certificateResult.issuerPrincipal}")
            if (certificateResult.issuerContainsControlCharacters) {
                silenceableEcho("    * Contains Control Characters: ${true.formatYesNo()}".formatRed(cliConfig.consoleOutputConfig))
            } else {
                verboseEcho(EchoType.SIGNATURE_CERTIFICATE, "    * Contains Control Characters: ${false.formatYesNo()}")
            }
        } else {
            verboseEcho(EchoType.SIGNATURE_CERTIFICATE, "  * Issuer:")
            verboseEcho(EchoType.SIGNATURE_CERTIFICATE, "    * Principal: ${certificateResult.issuerPrincipal}")
            verboseEcho(EchoType.SIGNATURE_CERTIFICATE, "    * Contains Control Characters: ${false.formatYesNo()}")
        }

        silenceableEcho("  * Subject:")
        silenceableEcho("    * ${certificateResult.subjectPrincipal}")
        if (certificateResult.subjectContainsControlCharacters) {
            silenceableEcho("    * Contains Control Characters: ${true.formatYesNo()}".formatRed(cliConfig.consoleOutputConfig))
        } else {
            verboseEcho(EchoType.SIGNATURE_CERTIFICATE, "    * Contains Control Characters: ${false.formatYesNo()}")
        }
    }

    private fun printAndroidSigningBlockResult(signingBlockResult: SigningBlockResult) {
        silenceableEcho("Android Signing Block verification:")
        silenceableEcho("-----------------------------------")

        mapOf(
            "OK" to AndroidSigningBlock.getOkBlocks(),
        ).forEach {
            verboseEcho(EchoType.SIGNING_BLOCK, "* ${it.key} blocks:")
            it.value.forEach { (key, value) ->
                val hasBlock = signingBlockResult.blocks.contains(key)
                verboseEcho(EchoType.SIGNING_BLOCK, "  * Has \"${value}\" block (${key.formatAsHex()}): ${hasBlock.formatYesNo()}")
            }
        }

        val googleBlocks = AndroidSigningBlock.getGoogleBlocks()
        val hasGoogleBlock = signingBlockResult.badBlocks.any { googleBlocks.contains(it) }

        mapOf(
            "Google" to googleBlocks,
        ).forEach { printSigningBlock(signingBlockResult.blocks, it.key, it.value, hasGoogleBlock.not(), false) }

        val payloadBlocks = AndroidSigningBlock.getPayloadBlocks()
        val hasPayloadBlock = signingBlockResult.badBlocks.any { payloadBlocks.contains(it) }

        mapOf(
            "Payload" to payloadBlocks,
        ).forEach { printSigningBlock(signingBlockResult.blocks, it.key, it.value, hasPayloadBlock.not(), true) }

        val unknownBlocks = signingBlockResult.unknownBlocksFormatted
        if (unknownBlocks.isEmpty()) {
            verboseEcho(EchoType.SIGNING_BLOCK, "* Unknown blocks:")
            verboseEcho(EchoType.SIGNING_BLOCK, "  * No unknown blocks")
            verboseEcho(EchoType.SIGNING_BLOCK)
        } else {
            silenceableEcho("* Unknown blocks:")
            unknownBlocks.forEach { silenceableEcho("  * $it".formatRed(cliConfig.consoleOutputConfig)) }
            silenceableEcho()
        }

        if (signingBlockResult.badBlocks.isEmpty()) {
            silenceableEcho("No offending blocks found.".formatGreen(cliConfig.consoleOutputConfig))
        }

        silenceableEcho()
    }

    private fun printSigningBlock(blocks: Set<Int>, blockType: String, blockMap: Map<Int, String>, verbose: Boolean, critical: Boolean) {
        val typeMessage = "* $blockType blocks:"
        if (verbose) {
            verboseEcho(EchoType.SIGNING_BLOCK, typeMessage)
        } else {
            silenceableEcho(typeMessage)
        }

        blockMap.forEach { (key, value) ->
            val hasBlock = blocks.contains(key)
            val blockMessage = "  * Has \"${value}\" block (${key.formatAsHex()}): ${hasBlock.formatYesNo()}"
            if (hasBlock) {
                if (critical) {
                    silenceableEcho(blockMessage.formatRed(cliConfig.consoleOutputConfig))
                } else {
                    silenceableEcho(blockMessage.formatYellow(cliConfig.consoleOutputConfig))
                }
            } else {
                verboseEcho(EchoType.SIGNING_BLOCK, blockMessage)
            }
        }
    }

    private fun formatAntiFeatures(antiFeatures: Array<String>): String = buildString {
        val antiFeatureIterator = antiFeatures.iterator()
        while (antiFeatureIterator.hasNext()) {
            append(antiFeatureIterator.next())
            if (antiFeatureIterator.hasNext()) {
                append(",")
            }
        }
    }.formatBold(cliConfig.consoleOutputConfig)

    private fun storeScanResultAsJsonIfWanted(apkFile: File, scanResult: ApkScanResult) {
        val excludeDefaults = jsonExcludeDefaults ?: cliConfig.scanApkConfig.jsonExcludeDefaults

        val scanResultJsonString = when (storeAsJson ?: cliConfig.scanApkConfig.storeAsJson) {
            OutputStoreType.YES -> {
                @Suppress("JSON_FORMAT_REDUNDANT")
                Json { encodeDefaults = excludeDefaults.not() }.encodeToString(scanResult)
            }

            OutputStoreType.PRETTY -> {
                @Suppress("JSON_FORMAT_REDUNDANT")
                Json { encodeDefaults = excludeDefaults.not(); prettyPrint = true }.encodeToString(scanResult)
            }

            else -> {
                // Do nothing
                ""
            }
        }

        if (scanResultJsonString.isNotEmpty()) {
            val resultOutputName = "${apkFile.name}.json"
            val resultOutputDirectory: File = if (outputResultWithApk ?: cliConfig.scanApkConfig.jsonOutputWithApk) {
                apkFile.absoluteFile.parentFile
            } else {
                val jsonOutputDirectory = jsonResultOutputDirectory ?: File(cliConfig.scanApkConfig.jsonOutputDirectory).absoluteFile
                if (jsonOutputSubdirectory ?: cliConfig.scanApkConfig.jsonOutputSubdirectory) {
                    val resultOutputDirectory = File(jsonOutputDirectory, scanStartedAt.format(localDateTimeFormatter))
                    resultOutputDirectory.mkdirs()
                    resultOutputDirectory
                } else {
                    jsonOutputDirectory
                }
            }

            val resultOutputFile = File(resultOutputDirectory, resultOutputName)

            verboseEcho(EchoType.GENERIC, "Writing scan result output to: ${resultOutputFile.absolutePath}")
            verboseEcho(EchoType.GENERIC)

            resultOutputFile.writeText(scanResultJsonString)
        }
    }

    private enum class EchoType {
        GENERIC,
        APK_INFO,
        DETECTED_LIBRARIES,
        PERMISSIONS,
        SIGNATURE_APKSIG,
        SIGNATURE_CERTIFICATE,
        SIGNING_BLOCK,
    }

    private fun verboseEcho(
        echoType: EchoType = EchoType.GENERIC,
        message: Any? = "",
        trailingNewline: Boolean = true,
        err: Boolean = false,
        forcePrint: Boolean = false,
    ) {
        val verbose = forcePrint || cliConfig.scanApkConfig.verboseAll || when (echoType) {
            EchoType.GENERIC -> cliConfig.scanApkConfig.verboseGeneric
            EchoType.APK_INFO -> cliConfig.scanApkConfig.verboseApkInfo
            EchoType.DETECTED_LIBRARIES -> cliConfig.scanApkConfig.verboseDetectedLibraries
            EchoType.PERMISSIONS -> cliConfig.scanApkConfig.verbosePermissions
            EchoType.SIGNATURE_APKSIG -> cliConfig.scanApkConfig.verboseSignatureApksig
            EchoType.SIGNATURE_CERTIFICATE -> cliConfig.scanApkConfig.verboseSignatureCertificate
            EchoType.SIGNING_BLOCK -> cliConfig.scanApkConfig.verboseSigningBlock
        }
        if (cliConfig.verbose || verbose) {
            silenceableEcho(message, trailingNewline, err)
        }
    }
}
