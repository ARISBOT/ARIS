/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.extensions

import eu.katastima.apkscanner.config.ApkScannerConfig
import eu.katastima.apkscanner.database.dao.SigningCertificateAllowlistEntity
import eu.katastima.apkscanner.database.dao.SigningCertificateAllowlistTable
import eu.katastima.apkscanner.database.dao.SigningCertificateDenylistEntity
import eu.katastima.apkscanner.database.dao.SigningCertificateDenylistTable
import eu.katastima.apkscanner.models.signing.SigningCertificate
import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.security.cert.X509Certificate

fun X509Certificate.isAllowListed(database: Database, apkScannerConfig: ApkScannerConfig): Pair<Boolean, SigningCertificate> {
    return transaction(database) {
        if (apkScannerConfig.databaseConfig.debug) {
            addLogger(StdOutSqlLogger)
        }

        val sha256Iterator = SigningCertificateAllowlistEntity.find { SigningCertificateAllowlistTable.sha256 eq encoded.toSha256() }
        if (!sha256Iterator.empty()) {
            return@transaction Pair(true, sha256Iterator.first().toSigningCertificate())
        }

        val sha1Iterator = SigningCertificateAllowlistEntity.find { SigningCertificateAllowlistTable.sha1 eq encoded.toSha1() }
        if (!sha1Iterator.empty()) {
            return@transaction Pair(true, sha1Iterator.first().toSigningCertificate())
        }

        val md5Iterator = SigningCertificateAllowlistEntity.find { SigningCertificateAllowlistTable.md5 eq encoded.toMd5() }
        if (!md5Iterator.empty()) {
            return@transaction Pair(true, md5Iterator.first().toSigningCertificate())
        }

        return@transaction Pair(false, SigningCertificate())
    }
}

fun X509Certificate.isDenyListed(database: Database, apkScannerConfig: ApkScannerConfig): Pair<Boolean, SigningCertificate> {
    return transaction(database) {
        if (apkScannerConfig.databaseConfig.debug) {
            addLogger(StdOutSqlLogger)
        }

        val sha256Iterator = SigningCertificateDenylistEntity.find { SigningCertificateDenylistTable.sha256 eq encoded.toSha256() }
        if (!sha256Iterator.empty()) {
            return@transaction Pair(true, sha256Iterator.first().toSigningCertificate())
        }

        val sha1Iterator = SigningCertificateDenylistEntity.find { SigningCertificateDenylistTable.sha1 eq encoded.toSha1() }
        if (!sha1Iterator.empty()) {
            return@transaction Pair(true, sha1Iterator.first().toSigningCertificate())
        }

        val md5Iterator = SigningCertificateDenylistEntity.find { SigningCertificateDenylistTable.md5 eq encoded.toMd5() }
        if (!md5Iterator.empty()) {
            return@transaction Pair(true, md5Iterator.first().toSigningCertificate())
        }

        return@transaction Pair(false, SigningCertificate())
    }
}
