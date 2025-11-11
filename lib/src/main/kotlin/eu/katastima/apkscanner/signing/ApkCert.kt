/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.signing

import com.android.apksig.ApkVerifier
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.security.cert.X509Certificate
import java.util.logging.Level
import java.util.logging.Logger

class ApkCert(private val apkFile: File) {

    private var signingBlockValueIdMap: MutableMap<Int, ByteBuffer>? = null

    fun verify(): VerificationResult {
        var verificationResult = VerificationResult()

        // Verify signature block
        verificationResult = verificationResult.copy(
            signatureBlockVerificationResult = verifySignatureBlock(),
        )

        try {
            val builder = ApkVerifier.Builder(apkFile)
            val result = builder.build().verify()
            if (result.isVerified) {
                return verificationResult.copy(
                    verifiedByApkSig = result.isVerified,
                    v1 = result.isVerifiedUsingV1Scheme,
                    v2 = result.isVerifiedUsingV2Scheme,
                    v3 = result.isVerifiedUsingV3Scheme,
                    v31 = result.isVerifiedUsingV31Scheme,
                    v4 = result.isVerifiedUsingV4Scheme,
                    sourceStampVerified = result.isSourceStampVerified,
                    certificates = result.signerCertificates,
                )
            } else {
                for (error in result.errors) {
                    LOGGER.log(Level.SEVERE, "Error: $error")
                }
            }
        } catch (e: Exception) {
            LOGGER.log(Level.SEVERE, "Could not verify APK (${apkFile})", e)
        }

        return verificationResult
    }

    private fun verifySignatureBlock(): SignatureBlockVerificationResult {
        var signatureBlockVerificationResult = SignatureBlockVerificationResult()
        readAndroidSigningBlock()

        val valueIdMap = signingBlockValueIdMap?.toMap() ?: return signatureBlockVerificationResult

        // Check for dependency info block
        val dependencyInfoBlockBuffer = valueIdMap[AndroidSigningBlockUtil.DEPENDENCY_INFO_BLOCK_ID]
        if (dependencyInfoBlockBuffer != null) {
            signatureBlockVerificationResult = signatureBlockVerificationResult.copy(
                hasDependencyInfoBlock = true,
                dependencyInfoBlockValue = AndroidSigningBlockUtil.getString(dependencyInfoBlockBuffer)
            )
        }

        // TODO: add more checks

        return signatureBlockVerificationResult
    }

    private fun readAndroidSigningBlock(): MutableMap<Int, ByteBuffer>? {
        if (signingBlockValueIdMap.isNullOrEmpty()) {
            RandomAccessFile(apkFile, "r").use { randomAccessFile ->
                randomAccessFile.channel.use { fileChannel ->
                    try {
                        val androidSigningBlockPair = AndroidSigningBlockUtil.findApkSigningBlock(fileChannel)
                        signingBlockValueIdMap = AndroidSigningBlockUtil.getIdValuePairs(androidSigningBlockPair.first)
                    } catch (exc: Exception) {
                        LOGGER.log(Level.SEVERE, "Could not read APK signing block", exc)
                    }
                }
            }
        }
        return signingBlockValueIdMap
    }

    companion object {
        private val LOGGER = Logger.getLogger(ApkCert::class.simpleName)
    }
}

data class SignatureBlockVerificationResult(
    val hasDependencyInfoBlock: Boolean = false,
    val dependencyInfoBlockValue: String = "",
)

data class VerificationResult(
    /** Whether [apksig](https://android.googlesource.com/platform/tools/apksig/) thinks the signature is verified. */
    val verifiedByApkSig: Boolean = false,
    /** [Link](https://source.android.com/docs/security/features/apksigning#v1) */
    val v1: Boolean = false,
    /** [Link](https://source.android.com/docs/security/features/apksigning/v2) */
    val v2: Boolean = false,
    /** [Link](https://source.android.com/docs/security/features/apksigning/v3) */
    val v3: Boolean = false,
    /** [Link](https://source.android.com/docs/security/features/apksigning/v3-1) */
    val v31: Boolean = false,
    /**
     * Requires a valid v2 and v3.
     *
     * [Link](https://source.android.com/docs/security/features/apksigning/v4)
     */
    val v4: Boolean = false,
    /**
     * SourceStamp improves traceability of apps with respect to unauthorized distribution.
     * The stamp is part of the APK that is protected by the signing block.
     * The APK contents hash is signed using the stamp key, and is saved as part of the signing block.
     *
     * [Link](https://android.googlesource.com/platform/frameworks/base/+/5fae7f3900a42fc91affdd951978b6099a13c3f4/core/java/android/util/apk/SourceStampVerifier.java)
     */
    val sourceStampVerified: Boolean = false,
    /** The certificates of the signer. */
    val certificates: List<X509Certificate> = emptyList(),
    /** The result of the signature block verification */
    val signatureBlockVerificationResult: SignatureBlockVerificationResult = SignatureBlockVerificationResult(),
) {

    fun isInvalid(): Boolean = (!v1 && !v2 && !v3) || certificates.isEmpty()
}
