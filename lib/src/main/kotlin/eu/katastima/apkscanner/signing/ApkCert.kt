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

    fun verify(database: Database, apkScannerConfig: ApkScannerConfig): VerificationResult {
        var verificationResult = VerificationResult()

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

            verificationResult = verificationResult.copy(
                apkSigResult = apkSigResult,
                certificates = result.signerCertificates,
            )
        } catch (e: Exception) {
            LOGGER.log(Level.SEVERE, "Could not verify APK (${apkFile})", e)
        }

        val certificateResults = mutableListOf<CertificateResult>()
        verificationResult.certificates.forEach { certificate ->
            val publicKeyResult = PublicKeyResult(
                keyAlgorithm = certificate.publicKey.algorithm,
                keySizeBits = certificate.publicKey.getPublicKeySize(),
                sha256 = certificate.publicKey.encoded.toSha256(),
                sha1 = certificate.publicKey.encoded.toSha1(),
                md5 = certificate.publicKey.encoded.toMd5(),
            )
            val certificateResult = CertificateResult(
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
            certificateResults.add(certificateResult)
        }
        verificationResult = verificationResult.copy(
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
        verificationResult = verificationResult.copy(
            signingBlockResult = signingBlockResult,
        )

        return verificationResult
    }

    companion object {
        private val LOGGER = Logger.getLogger(ApkCert::class.simpleName)
    }
}
