/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.models.signing

import java.security.cert.X509Certificate

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
) {

    fun isInvalid(): Boolean = (!v1 && !v2 && !v3) || certificates.isEmpty()
}
