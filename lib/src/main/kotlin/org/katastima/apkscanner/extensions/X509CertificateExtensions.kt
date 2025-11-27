/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.extensions

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.katastima.apkscanner.data.certificate.CertificateRepository
import org.katastima.apkscanner.models.signing.SigningCertificate
import java.security.PublicKey
import java.security.cert.X509Certificate
import java.security.interfaces.DSAKey
import java.security.interfaces.ECKey
import java.security.interfaces.RSAKey

suspend fun X509Certificate.isDenyListed(
    certificateRepository: CertificateRepository,
    backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
): Set<SigningCertificate> = withContext(backgroundDispatcher) {
    val denylistMatches: MutableSet<SigningCertificate> = mutableSetOf()

    val encodedSha256 = encoded.toSha256()
    val encodedSha1 = encoded.toSha1()
    val encodedMd5 = encoded.toMd5()

    // e.g.: [C=US, CN=Android Debug, O=Android]
    val dnSplitList = subjectX500Principal.name.split(",").sorted()

    certificateRepository.getAll()
        .filter { it.name != "TEMPLATE_ENTRY" }
        .sortedBy { it.name }
        .forEach { denylistEntry ->
            // If our hashes match any of theirs, add to denylist matches and skip checking DNs.
            if (denylistEntry.sha256.contains(encodedSha256)
                || denylistEntry.sha1.contains(encodedSha1)
                || denylistEntry.md5.contains(encodedMd5)
            ) {
                denylistMatches.add(denylistEntry)
                return@forEach
            }

            denylistEntry.dn.forEach { dnEntry ->
                // e.g.: [C=US, CN=Android, L=Mountain View, O=Android, OU=Android, ST=California, emailAddress=android@android.com]
                val entityDnSplitList = dnEntry.split("/").filter { it.isNotBlank() }.sorted()
                if (entityDnSplitList == dnSplitList) {
                    denylistMatches.add(denylistEntry)
                }
            }
        }

    return@withContext denylistMatches
}

fun PublicKey.getPublicKeySize(): Int = when (this) {
    is RSAKey -> {
        (this as RSAKey).modulus.bitLength()
    }

    is ECKey -> {
        (this as ECKey).params.order.bitLength()
    }

    is DSAKey -> {
        // DSA parameters may be inherited from the certificate. We
        // don't handle this case at the moment.
        (this as DSAKey).params?.p?.bitLength() ?: -1
    }

    else -> {
        -1
    }
}
