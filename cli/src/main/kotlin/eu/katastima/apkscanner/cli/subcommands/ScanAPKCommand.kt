/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.cli.subcommands

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.types.file
import eu.katastima.apkscanner.extensions.toMd5
import eu.katastima.apkscanner.extensions.toSha1
import eu.katastima.apkscanner.extensions.toSha256
import eu.katastima.apkscanner.models.LibraryInformation
import eu.katastima.apkscanner.scanapk.ApkScanResult
import eu.katastima.apkscanner.scanapk.ScanAPK
import java.io.File
import java.security.PublicKey
import java.security.interfaces.DSAKey
import java.security.interfaces.ECKey
import java.security.interfaces.RSAKey


class ScanAPKCommand : SuspendingCliktCommand() {

    private val apkFiles: List<File> by argument("apk")
        .file(mustExist = true, mustBeReadable = true, canBeDir = false)
        .help("A single or multiple APK files which should get scanned")
        .multiple(true)

    override val printHelpOnEmptyArgs = true

    private var hasOffendingLibrary: Boolean = false

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

        if (hasOffendingLibrary) {
            throw ProgramResult(1)
        }
    }

    private fun printScanResult(apkFile: File, scanResult: ApkScanResult) {
        echo("Scanned APK:")
        echo("------------")

        echo("* File: ${apkFile.absolutePath}")
        echo("* SHA-256: ${apkFile.toSha256()}")
        echo()

        printLibraryResult(scanResult)
        printSignatureVerificationResult(scanResult)

        echo("==============================================================================")
        echo()
    }

    private fun printLibraryResult(scanResult: ApkScanResult) {
        // Store offending libraries in an own list to prevent having to iterate through the whole list multiple times.
        val offendingLibraries: MutableList<LibraryInformation> = mutableListOf()

        echo("Libraries detected:")
        echo("-------------------")
        scanResult.detectedLibraries.forEach { library ->
            echo("* ${library.name} (${library.libraryId}): ${library.type}, ${library.license}", trailingNewline = false)

            if (library.antiFeatures.isNotEmpty()) {
                echo("; ${formatAntiFeatures(library.antiFeatures)}", trailingNewline = false)
                offendingLibraries.add(library)
            }
            echo()
        }
        echo()

        if (offendingLibraries.isEmpty()) {
            echo("No offending libraries found.")
        } else {
            hasOffendingLibrary = true

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

        val verificationResult = scanResult.verificationResult
        val signatureBlockVerificationResult = scanResult.verificationResult.signatureBlockVerificationResult

        if (verificationResult.isInvalid()) {
            echo("Failed to verify signature, please ensure the APK is properly signed!")
            echo()
            return
        }

        // Print whether signature versions v1, v2 or v3 are valid.
        // v1: https://source.android.com/docs/security/features/apksigning#v1
        // v2: https://source.android.com/docs/security/features/apksigning/v2
        // v3: https://source.android.com/docs/security/features/apksigning/v3
        // v3.1: https://source.android.com/docs/security/features/apksigning/v3-1
        // v4: https://source.android.com/docs/security/features/apksigning/v4
        echo("* apksig thinks this certificate is: ${if (verificationResult.verifiedByApkSig) "valid" else "invalid"}")
        echo("* Source Stamp: ${if (verificationResult.sourceStampVerified) "valid" else "invalid"}")
        echo("* v1: ${if (verificationResult.v1) "valid" else "invalid"}")
        echo("* v2: ${if (verificationResult.v2) "valid" else "invalid"}")
        echo("* v3: ${if (verificationResult.v3) "valid" else "invalid"}")
        echo("* v3.1: ${if (verificationResult.v31) "valid" else "invalid"}")
        echo("* v4: ${if (verificationResult.v4) "valid" else "invalid"}")
        echo("  ------------------------------------------")
        echo("* Has dependency info block: ${if (signatureBlockVerificationResult.hasDependencyInfoBlock) "Yes" else "No"}")
        echo("  ------------------------------------------")
        echo("* Number of certificates: ${verificationResult.certificates.size}")

        var certificateCounter = 1
        verificationResult.certificates.forEach { certificate ->
            echo("* Certificate #${certificateCounter}")
            echo("  * Key Algorithm Name: ${certificate.sigAlgName}")
            echo("  * Key Algorithm OID:  ${certificate.sigAlgOID}")
            echo("  * Issuer Principal:  ${certificate.issuerX500Principal}")
            echo("  * Subject Principal: ${certificate.subjectX500Principal}")
            echo("  * Not Before: ${certificate.notBefore}")
            echo("  * Not After:  ${certificate.notAfter}")
            echo("  * SHA-256: ${certificate.encoded.toSha256()}")
            echo("  * SHA-1:   ${certificate.encoded.toSha1()}")
            echo("  * MD5:     ${certificate.encoded.toMd5()}")
            echo("    --------------------------------------------------------------------------")
            echo("  * Public Key - Algorithm: ${certificate.publicKey.algorithm}")
            echo("  * Public Key - Key Size (bits): ${getPublicKeySize(certificate.publicKey)}")
            echo("  * SHA-256: ${certificate.publicKey.encoded.toSha256()}")
            echo("  * SHA-1:   ${certificate.publicKey.encoded.toSha1()}")
            echo("  * MD5:     ${certificate.publicKey.encoded.toMd5()}")
            certificate.publicKey
            certificateCounter++
        }

        echo()
    }

    private fun getPublicKeySize(publicKey: PublicKey): Int = when (publicKey) {
        is RSAKey -> {
            (publicKey as RSAKey).modulus.bitLength()
        }

        is ECKey -> {
            (publicKey as ECKey).params.order.bitLength()
        }

        is DSAKey -> {
            // DSA parameters may be inherited from the certificate. We
            // don't handle this case at the moment.
            (publicKey as DSAKey).params?.p?.bitLength() ?: -1
        }

        else -> {
            -1
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
}
