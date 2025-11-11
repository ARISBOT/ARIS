/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.signing

import com.android.apksig.ApkVerifier
import eu.katastima.apkscanner.models.signing.VerificationResult
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger

class ApkCert(private val apkFile: File) {

    fun verify(): VerificationResult {
        var verificationResult = VerificationResult()

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

    companion object {
        private val LOGGER = Logger.getLogger(ApkCert::class.simpleName)
    }
}
