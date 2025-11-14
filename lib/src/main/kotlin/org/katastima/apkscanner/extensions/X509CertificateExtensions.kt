/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.extensions

import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.katastima.apkscanner.database.dao.SigningCertificateDenylistEntity
import org.katastima.apkscanner.database.dao.SigningCertificateDenylistTable
import org.katastima.apkscanner.models.signing.SigningCertificate
import java.security.PublicKey
import java.security.cert.X509Certificate
import java.security.interfaces.DSAKey
import java.security.interfaces.ECKey
import java.security.interfaces.RSAKey

fun X509Certificate.isDenyListed(database: Database, debugDatabase: Boolean = false): Set<SigningCertificate> {
    val denylistMatches: MutableSet<SigningCertificate> = mutableSetOf()

    val encodedSha256 = encoded.toSha256()
    val encodedSha1 = encoded.toSha1()
    val encodedMd5 = encoded.toMd5()

    // e.g.: [C=US, CN=Android Debug, O=Android]
    val dnSplitList = subjectX500Principal.name.split(",").sorted()

    transaction(database) {
        if (debugDatabase) {
            addLogger(StdOutSqlLogger)
        }

        SigningCertificateDenylistTable
            .selectAll()
            .where { SigningCertificateDenylistTable.name neq "TEMPLATE_ENTRY" }
            .map { SigningCertificateDenylistEntity.wrapRow(it) }
            .forEach { denylistEntity ->
                // If our hashes match any of theirs, add to denylist matches and skip checking DNs.
                if (denylistEntity.sha256.contains(encodedSha256)
                    || denylistEntity.sha1.contains(encodedSha1)
                    || denylistEntity.md5.contains(encodedMd5)
                ) {
                    denylistMatches.add(denylistEntity.toSigningCertificate())
                    return@forEach
                }

                denylistEntity.dn.forEach { dnEntry ->
                    // e.g.: [C=US, CN=Android, L=Mountain View, O=Android, OU=Android, ST=California, emailAddress=android@android.com]
                    val entityDnSplitList = dnEntry.split("/").filter { it.isNotBlank() }.sorted()
                    if (entityDnSplitList == dnSplitList) {
                        denylistMatches.add(denylistEntity.toSigningCertificate())
                    }
                }
            }
    }

    return denylistMatches
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
