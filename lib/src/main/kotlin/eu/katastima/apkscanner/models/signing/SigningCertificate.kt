/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.models.signing

import kotlinx.serialization.Serializable

@Serializable
data class SigningCertificate(
    /** (required) The name of a certificate, used by humans to identify it */
    val name: String,
    /** (optional) The description of a certificate, used by humans to identify it */
    val description: String = "",
    /** (optional) The distinguished name of the certificate. Different entities are separated by ",". For example: C=US, O=Android, CN=Android Debug */
    val dn: String = "",
    /** (optional) The SHA256 of a certificate. Note: any of the has options must be set to actually be able to identify it! */
    val sha256: String = "",
    /** (optional) The SHA1 of a certificate. Note: any of the has options must be set to actually be able to identify it! */
    val sha1: String = "",
    /** (optional) The MD5 of a certificate. Note: any of the has options must be set to actually be able to identify it! */
    val md5: String = "",
)
