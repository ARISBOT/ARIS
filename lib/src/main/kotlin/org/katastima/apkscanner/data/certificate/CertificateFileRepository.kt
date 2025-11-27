/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.data.certificate

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import org.katastima.apkscanner.config.DataConfig
import org.katastima.apkscanner.data.DataUtil
import org.katastima.apkscanner.models.signing.SigningCertificate
import org.slf4j.LoggerFactory

class CertificateFileRepository(
    private val dataConfig: DataConfig,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : CertificateRepository {

    private val json = Json { ignoreUnknownKeys = true }

    private val signingCertificates: List<SigningCertificate> by lazy {
        val denyList: MutableList<SigningCertificate> = mutableListOf()
        try {
            val certificateDataContent = DataUtil.getCertificateConfigContent(dataConfig)

            val jsonElement = json.parseToJsonElement(certificateDataContent)
            jsonElement.jsonArray.forEach { denyList.add(json.decodeFromJsonElement<SigningCertificate>(it)) }
        } catch (exc: Exception) {
            LOGGER.error("Could not get certificate data", exc)
        }
        denyList
    }

    override suspend fun getAll(): List<SigningCertificate> = withContext(ioDispatcher) {
        return@withContext signingCertificates
    }

    override suspend fun countAll(): Long = withContext(ioDispatcher) {
        return@withContext signingCertificates.size.toLong()
    }

    override suspend fun add(signingCertificate: SigningCertificate): Result<Long> = withContext(ioDispatcher) {
        return@withContext Result.failure(RuntimeException("Not implemented yet"))
    }

    override suspend fun update(signingCertificate: SigningCertificate): Result<Long> = withContext(ioDispatcher) {
        return@withContext Result.failure(RuntimeException("Not implemented yet"))
    }

    override suspend fun delete(signingCertificate: SigningCertificate): Result<Long> = withContext(ioDispatcher) {
        return@withContext Result.failure(RuntimeException("Not implemented yet"))
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(CertificateFileRepository::class.java)
    }
}
