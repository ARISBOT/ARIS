/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.data.certificate

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.katastima.apkscanner.database.dao.certificate.SigningCertificateDenylistEntity
import org.katastima.apkscanner.database.dao.certificate.SigningCertificateDenylistTable
import org.katastima.apkscanner.models.signing.SigningCertificate

class CertificateDatabaseRepository(
    private val database: Database,
    private val backgroundDispatcher: CoroutineDispatcher,
    private val debugDatabase: Boolean = false,
) : CertificateRepository {

    override suspend fun getAll(): List<SigningCertificate> = dbQuery {
        return@dbQuery SigningCertificateDenylistEntity
            .all()
            .sortedBy { SigningCertificateDenylistTable.name }
            .map { it.toSigningCertificate() }
    }

    override suspend fun countAll(): Long = dbQuery {
        return@dbQuery SigningCertificateDenylistEntity
            .all()
            .count()
    }

    override suspend fun add(signingCertificate: SigningCertificate): Result<Long> = dbQuery {
        val hasCertificateWithName = SigningCertificateDenylistEntity
            .find { SigningCertificateDenylistTable.name eq signingCertificate.name }
            .empty().not()
        if (hasCertificateWithName) {
            return@dbQuery Result.failure(kotlin.RuntimeException("Certificate with name (${signingCertificate.name}) already exists!"))
        }

        try {
            val newSigningCertificateEntry = SigningCertificateDenylistEntity.new {
                name = signingCertificate.name
                description = signingCertificate.description
                sourceUrl = signingCertificate.sourceUrl
                dn = signingCertificate.dn.sorted()
                sha256 = signingCertificate.sha256.sorted()
                sha1 = signingCertificate.sha1.sorted()
                md5 = signingCertificate.md5.sorted()
            }
            return@dbQuery Result.success(newSigningCertificateEntry.id.value.toLong())
        } catch (exc: Exception) {
            return@dbQuery Result.failure(exc)
        }
    }

    override suspend fun update(signingCertificate: SigningCertificate): Result<Long> = dbQuery {
        try {
            val updatedEntity = SigningCertificateDenylistEntity
                .findSingleByAndUpdate(SigningCertificateDenylistTable.name eq signingCertificate.name) {
                    it.description = signingCertificate.description
                    it.sourceUrl = signingCertificate.sourceUrl
                    it.dn = signingCertificate.dn.sorted()
                    it.sha256 = signingCertificate.sha256.sorted()
                    it.sha1 = signingCertificate.sha1.sorted()
                    it.md5 = signingCertificate.md5.sorted()
                }
            if (updatedEntity == null) {
                return@dbQuery Result.failure(kotlin.RuntimeException("Certificate with name (${signingCertificate.name}) does not exist!"))
            }
            return@dbQuery Result.success(updatedEntity.id.value.toLong())
        } catch (exc: Exception) {
            return@dbQuery Result.failure(exc)
        }
    }

    override suspend fun delete(signingCertificate: SigningCertificate): Result<Long> = dbQuery {
        try {
            val updatedEntity = SigningCertificateDenylistEntity
                .findSingleByAndUpdate(SigningCertificateDenylistTable.name eq signingCertificate.name) {
                    it.delete()
                }
            if (updatedEntity == null) {
                return@dbQuery Result.failure(kotlin.RuntimeException("Certificate with name (${signingCertificate.name}) does not exist!"))
            }
            return@dbQuery Result.success(updatedEntity.id.value.toLong())
        } catch (exc: Exception) {
            return@dbQuery Result.failure(exc)
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T = withContext(backgroundDispatcher) {
        return@withContext suspendTransaction(database) {
            if (debugDatabase) {
                addLogger(StdOutSqlLogger)
            }
            return@suspendTransaction block()
        }
    }
}
