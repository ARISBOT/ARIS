/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.models.signing

import kotlinx.serialization.Serializable

/**
 * Represents an entry for a signing certificate entity.
 *
 * This is for example used for checking matches of the signing certificate of an APK with the specified denylist.
 *
 * If ANY of dn, sha256, sha1 or md5 match, the signing certificate is assumed to be denylisted.
 *
 * Note: any of dn, sha256, sha1 or md5 must be set to actually be able to identify it!
 */
@Serializable
data class SigningCertificate(
    /** (required) The name of a certificate, used by humans to identify it */
    val name: String,
    /** (optional) The description of a certificate, used by humans to identify it */
    val description: String = "",
    /** (optional) An URL for additional documentation purposes, e.g.: where the certificate was documented to be leaked. */
    val sourceUrl: String = "",
    /** (optional) The distinguished names of the certificate. Different entities are separated by "/". For example: /C=US/O=Android/CN=Android Debug */
    val dn: Set<String> = emptySet(),
    /** (optional) The SHA256 hashes of a certificate. */
    val sha256: Set<String> = emptySet(),
    /** (optional) The SHA1 hashes of a certificate. */
    val sha1: Set<String> = emptySet(),
    /** (optional) The MD5 hashes of a certificate. */
    val md5: Set<String> = emptySet(),
)
