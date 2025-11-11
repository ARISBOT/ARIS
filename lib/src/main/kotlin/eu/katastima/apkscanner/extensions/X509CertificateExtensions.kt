/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.extensions

import eu.katastima.apkscanner.config.ApkScannerConfig
import eu.katastima.apkscanner.database.dao.SigningCertificateDenylistEntity
import eu.katastima.apkscanner.database.dao.SigningCertificateDenylistTable
import eu.katastima.apkscanner.models.signing.SigningCertificate
import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.security.PublicKey
import java.security.cert.X509Certificate
import java.security.interfaces.DSAKey
import java.security.interfaces.ECKey
import java.security.interfaces.RSAKey

fun X509Certificate.isDenyListed(database: Database, apkScannerConfig: ApkScannerConfig): Set<SigningCertificate> {
    val denylistMatches: MutableSet<SigningCertificate> = mutableSetOf()

    transaction(database) {
        if (apkScannerConfig.databaseConfig.debug) {
            addLogger(StdOutSqlLogger)
        }

        // TODO: check DN

        SigningCertificateDenylistEntity.find { SigningCertificateDenylistTable.sha256 eq encoded.toSha256() }.forEach {
            denylistMatches.add(it.toSigningCertificate())
        }
        SigningCertificateDenylistEntity.find { SigningCertificateDenylistTable.sha1 eq encoded.toSha1() }.forEach {
            denylistMatches.add(it.toSigningCertificate())
        }
        SigningCertificateDenylistEntity.find { SigningCertificateDenylistTable.md5 eq encoded.toMd5() }.forEach {
            denylistMatches.add(it.toSigningCertificate())
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
