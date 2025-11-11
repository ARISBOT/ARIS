/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.models.signing

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.security.cert.X509Certificate

@Serializable
data class VerificationResult(
    @SerialName("signingSchemas") val apkSigResult: ApkSigResult = ApkSigResult(),
    @SerialName("signingCertificates") val certificateResults: List<CertificateResult> = emptyList(),
    @SerialName("signingBlocks") val signingBlockResult: SigningBlockResult = SigningBlockResult(),
    /** The certificates of the signer. */
    @Transient val certificates: List<X509Certificate> = emptyList(),
) {

    fun isInvalid(): Boolean = apkSigResult.isInvalid() || certificates.isEmpty()
}

@Serializable
data class ApkSigResult(
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
) {
    fun isInvalid(): Boolean = (!v1 && !v2 && !v3 && !v31)
}

@Serializable
data class CertificateResult(
    val allowListed: Pair<Boolean, SigningCertificate?> = Pair(false, null),
    val denyListed: Pair<Boolean, SigningCertificate?> = Pair(false, null),
    val sigAlgorithmName: String = "",
    val sigAlgorithmOID: String = "",
    val issuerPrincipal: String = "",
    val subjectPrincipal: String = "",
    val notBefore: LocalDateTime = LocalDateTime(1970, 1, 1, 0, 0, 0, 0),
    val notAfter: LocalDateTime = LocalDateTime(1970, 1, 1, 0, 0, 0, 0),
    val sha256: String = "",
    val sha1: String = "",
    val md5: String = "",
    val publicKeyResult: PublicKeyResult = PublicKeyResult(),
)

@Serializable
data class PublicKeyResult(
    val keyAlgorithm: String = "",
    val keySizeBits: Int = 0,
    val sha256: String = "",
    val sha1: String = "",
    val md5: String = "",
)
