/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.data.manifest

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.katastima.apkscanner.config.DataConfig
import org.katastima.apkscanner.data.DataUtil
import org.katastima.apkscanner.models.manifest.config.ManifestConfig
import org.katastima.apkscanner.models.manifest.config.ManifestFilterConfigEntry
import org.katastima.apkscanner.models.manifest.config.ManifestFlagConfigEntry
import org.katastima.apkscanner.models.manifest.config.ManifestPermissionConfigEntry
import org.slf4j.LoggerFactory

class ManifestFileRepository(
    private val dataConfig: DataConfig,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ManifestRepository {

    private val json = Json { ignoreUnknownKeys = true }

    private val manifestConfig: ManifestConfig by lazy {
        try {
            val manifestConfigContent = DataUtil.getManifestConfigContent(dataConfig)
            val jsonElement = json.parseToJsonElement(manifestConfigContent)
            json.decodeFromJsonElement(jsonElement)
        } catch (exc: Exception) {
            LOGGER.error("Could not get manifest config", exc)
            ManifestConfig()
        }
    }

    override suspend fun getManifestConfig(): ManifestConfig = withContext(ioDispatcher) {
        return@withContext manifestConfig
    }

    override suspend fun getAllFlagGroups(): List<ManifestFlagConfigEntry> = withContext(ioDispatcher) {
        return@withContext manifestConfig.dangerousFlags.entries.toList()
    }

    override suspend fun countAllFlagGroups(): Long = withContext(ioDispatcher) {
        return@withContext getAllFlagGroups().size.toLong()
    }

    override suspend fun countAllFlags(): Long = withContext(ioDispatcher) {
        return@withContext getAllFlagGroups().fold(0) { count, entity -> count + entity.flags.count() }
    }

    override suspend fun addFlagGroup(manifestFlagGroup: ManifestFlagConfigEntry): Result<Long> = withContext(ioDispatcher) {
        return@withContext Result.failure(RuntimeException("Not implemented yet"))
    }

    override suspend fun updateFlagGroup(manifestFlagGroup: ManifestFlagConfigEntry): Result<Long> = withContext(ioDispatcher) {
        return@withContext Result.failure(RuntimeException("Not implemented yet"))
    }

    override suspend fun deleteFlagGroup(manifestFlagGroup: ManifestFlagConfigEntry): Result<Long> = withContext(ioDispatcher) {
        return@withContext Result.failure(RuntimeException("Not implemented yet"))
    }

    override suspend fun getAllIntentFilterGroups(): List<ManifestFilterConfigEntry> = withContext(ioDispatcher) {
        return@withContext manifestConfig.dangerousFilters.entries.toList()
    }

    override suspend fun countAllIntentFilterGroups(): Long = withContext(ioDispatcher) {
        return@withContext getAllIntentFilterGroups().size.toLong()
    }

    override suspend fun countAllIntentFilters(): Long = withContext(ioDispatcher) {
        return@withContext getAllIntentFilterGroups().fold(0) { count, entity -> count + entity.filters.count() }
    }

    override suspend fun getAllPermissionGroups(): List<ManifestPermissionConfigEntry> = withContext(ioDispatcher) {
        return@withContext manifestConfig.dangerousPermissions.entries.toList()
    }

    override suspend fun countAllPermissionGroups(): Long = withContext(ioDispatcher) {
        return@withContext getAllPermissionGroups().size.toLong()
    }

    override suspend fun countAllPermissions(): Long = withContext(ioDispatcher) {
        return@withContext getAllPermissionGroups().fold(0) { count, entity -> count + entity.permissions.count() }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ManifestFileRepository::class.java)
    }
}
