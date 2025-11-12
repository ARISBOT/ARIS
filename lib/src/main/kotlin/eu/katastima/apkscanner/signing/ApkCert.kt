/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.signing

import com.android.apksig.ApkVerifier
import eu.katastima.apkscanner.config.ApkScannerConfig
import eu.katastima.apkscanner.extensions.*
import eu.katastima.apkscanner.models.signing.*
import org.jetbrains.exposed.v1.jdbc.Database
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger

class ApkCert(private val apkFile: File) {

    fun verify(database: Database, apkScannerConfig: ApkScannerConfig): SigningCheckResult {
        var signingCheckResult = SigningCheckResult()

        try {
            val builder = ApkVerifier.Builder(apkFile)
            val result = builder.build().verify()

            val apkSigResult = ApkSigResult(
                verifiedByApkSig = result.isVerified,
                v1 = result.isVerifiedUsingV1Scheme,
                v2 = result.isVerifiedUsingV2Scheme,
                v3 = result.isVerifiedUsingV3Scheme,
                v31 = result.isVerifiedUsingV31Scheme,
                v4 = result.isVerifiedUsingV4Scheme,
                sourceStampVerified = result.isSourceStampVerified,
            )

            signingCheckResult = signingCheckResult.copy(
                apkSigResult = apkSigResult,
                certificates = result.signerCertificates,
            )
        } catch (e: Exception) {
            LOGGER.log(Level.SEVERE, "Could not verify APK (${apkFile})", e)
        }

        val certificateResults = mutableListOf<CertificateResult>()
        signingCheckResult.certificates.forEach { certificate ->
            val publicKeyResult = PublicKeyResult(
                keyAlgorithm = certificate.publicKey.algorithm,
                keySizeBits = certificate.publicKey.getPublicKeySize(),
                sha256 = certificate.publicKey.encoded.toSha256(),
                sha1 = certificate.publicKey.encoded.toSha1(),
                md5 = certificate.publicKey.encoded.toMd5(),
            )
            var certificateResult = CertificateResult(
                denylistMatches = certificate.isDenyListed(database, apkScannerConfig),
                sigAlgorithmName = certificate.sigAlgName,
                sigAlgorithmOID = certificate.sigAlgOID,
                issuerPrincipal = certificate.issuerX500Principal.toString(),
                subjectPrincipal = certificate.subjectX500Principal.toString(),
                notBefore = certificate.notBefore.toLocalDateTime(),
                notAfter = certificate.notAfter.toLocalDateTime(),
                sha256 = certificate.encoded.toSha256(),
                sha1 = certificate.encoded.toSha1(),
                md5 = certificate.encoded.toMd5(),
                publicKeyResult = publicKeyResult,
            )
            // Check for control characters.
            certificateResult = certificateResult.copy(
                issuerContainsControlCharacters = certificateResult.issuerPrincipal.containsControlCharacters(),
                subjectContainsControlCharacters = certificateResult.subjectPrincipal.containsControlCharacters(),
            )
            certificateResults.add(certificateResult)
        }
        signingCheckResult = signingCheckResult.copy(
            certificateResults = certificateResults,
        )

        val androidSigningBlock = AndroidSigningBlock(apkFile)
        val allBlocks = AndroidSigningBlock.getAllBlocks()
        val blocks = androidSigningBlock.getBlockSet()
        val unknownBlocks = androidSigningBlock.getUnknownBlockSet()

        val signingBlockResult = SigningBlockResult(
            blocks = blocks,
            blocksFormatted = blocks.associate { it.formatAsHex() to (allBlocks[it] ?: "") }.toSortedMap(),
            unknownBlocks = unknownBlocks,
            unknownBlocksFormatted = unknownBlocks.map { it.formatAsHex() }.sorted(),
        )
        signingCheckResult = signingCheckResult.copy(
            signingBlockResult = signingBlockResult,
        )

        return signingCheckResult
    }

    companion object {
        private val LOGGER = Logger.getLogger(ApkCert::class.simpleName)
    }
}
