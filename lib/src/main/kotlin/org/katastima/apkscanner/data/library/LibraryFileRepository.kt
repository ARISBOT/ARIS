/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.data.library

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.katastima.apkscanner.config.DataConfig
import org.katastima.apkscanner.data.DataUtil
import org.katastima.apkscanner.models.library.LegacyLibraryDefinition
import org.katastima.apkscanner.models.library.LegacyLibraryInformation
import org.slf4j.LoggerFactory

class LibraryFileRepository(
    private val dataConfig: DataConfig,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : LibraryRepository {

    private val json = Json { ignoreUnknownKeys = true }

    private val libraryDefinitionList: List<LegacyLibraryDefinition> by lazy {
        val definitionList: MutableList<LegacyLibraryDefinition> = mutableListOf()
        try {
            val definitionContent = DataUtil.getLibraryDefinitionContent(dataConfig)
            definitionContent.forEach {
                definitionList.add(json.decodeFromString<LegacyLibraryDefinition>(it))
            }
        } catch (exc: Exception) {
            LOGGER.error("Could not get library definition content", exc)
        }
        definitionList
    }

    private val libraryInformationList: List<LegacyLibraryInformation> by lazy {
        val informationList: MutableList<LegacyLibraryInformation> = mutableListOf()
        try {
            val informationContent = DataUtil.getLibraryInformationContent(dataConfig)
            informationContent.forEach {
                informationList.add(json.decodeFromString<LegacyLibraryInformation>(it))
            }
        } catch (exc: Exception) {
            LOGGER.error("Could not get library information content", exc)
        }
        informationList
    }

    override suspend fun getAllInformationEntries(): List<LegacyLibraryInformation> = withContext(ioDispatcher) {
        return@withContext libraryInformationList
    }

    override suspend fun countInformationEntries(): Long = withContext(ioDispatcher) {
        return@withContext libraryInformationList.size.toLong()
    }

    override suspend fun getAllDefinitionEntries(): List<LegacyLibraryDefinition> = withContext(ioDispatcher) {
        return@withContext libraryDefinitionList
    }

    override suspend fun countDefinitionEntries(): Long = withContext(ioDispatcher) {
        return@withContext libraryDefinitionList.size.toLong()
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(LibraryFileRepository::class.java)
    }
}
