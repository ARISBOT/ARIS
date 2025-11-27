/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.data.certificate

import org.katastima.apkscanner.models.signing.SigningCertificate

interface CertificateRepository {
    suspend fun getAll(): List<SigningCertificate>
    suspend fun countAll(): Long

    suspend fun add(signingCertificate: SigningCertificate): Result<Long>
    suspend fun update(signingCertificate: SigningCertificate): Result<Long>
    suspend fun delete(signingCertificate: SigningCertificate): Result<Long>
}
