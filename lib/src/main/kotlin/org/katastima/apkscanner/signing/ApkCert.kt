/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.signing

import com.android.apksig.ApkVerifier
import org.jetbrains.exposed.v1.jdbc.Database
import org.katastima.apkscanner.extensions.containsControlCharacters
import org.katastima.apkscanner.extensions.formatAsHex
import org.katastima.apkscanner.extensions.getPublicKeySize
import org.katastima.apkscanner.extensions.isDenyListed
import org.katastima.apkscanner.extensions.toLocalDateTime
import org.katastima.apkscanner.extensions.toMd5
import org.katastima.apkscanner.extensions.toSha1
import org.katastima.apkscanner.extensions.toSha256
import org.katastima.apkscanner.models.signing.ApkSigResult
import org.katastima.apkscanner.models.signing.CertificateResult
import org.katastima.apkscanner.models.signing.PublicKeyResult
import org.katastima.apkscanner.models.signing.SigningBlockResult
import org.katastima.apkscanner.models.signing.SigningCheckResult
import org.slf4j.LoggerFactory
import java.io.File

class ApkCert(private val apkFile: File) {

    fun verify(database: Database, debugDatabase: Boolean = false): SigningCheckResult {
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
            LOGGER.error("Could not verify APK (${apkFile})", e)
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
                denylistMatches = certificate.isDenyListed(database, debugDatabase),
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
        private val LOGGER = LoggerFactory.getLogger(ApkCert::class.java)
    }
}
