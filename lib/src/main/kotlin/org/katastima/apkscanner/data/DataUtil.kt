/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.data

import org.katastima.apkscanner.config.DataConfig
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File

object DataUtil {

    private val LOGGER = LoggerFactory.getLogger(DataUtil::class.java)

    fun getFileNameForDataType(dataType: DataType): String = when (dataType) {
        DataType.CERTIFICATE_CONFIG -> "/data/certificate_denylist.json"
        DataType.LIBRARY_DEFINITION -> "/data/libsmali.jsonl"
        DataType.LIBRARY_INFORMATION -> "/data/libinfo.jsonl"
        DataType.MANIFEST_CONFIG -> "/data/manifest_config.json"
    }

    fun getConfigPathForDataType(dataType: DataType, dataConfig: DataConfig): String = when (dataType) {
        DataType.CERTIFICATE_CONFIG -> dataConfig.certificateDenylistPath
        DataType.LIBRARY_DEFINITION -> dataConfig.libraryDefinitionPath
        DataType.LIBRARY_INFORMATION -> dataConfig.libraryInformationPath
        DataType.MANIFEST_CONFIG -> dataConfig.manifestConfigPath
    }

    fun getConfigContentReader(dataType: DataType, dataConfig: DataConfig): BufferedReader? {
        val configPath = getConfigPathForDataType(dataType, dataConfig)
        if (configPath.isBlank()) {
            if (dataConfig.useDefaultData) {
                LOGGER.trace("Using default data for {}", dataType)

                val contentFileName = getFileNameForDataType(dataType)
                val dataUrl = DataUtil::class.java.getResource(contentFileName)
                val dataReader = dataUrl?.openStream()?.bufferedReader()
                if (dataReader == null) {
                    LOGGER.warn("Could not read default resource ({})", contentFileName)
                }
                return dataReader
            }
            return null
        }

        val configFile = File(configPath)
        if (!configFile.exists()) {
            LOGGER.warn("Path for $dataType ({}) does not exist", configFile.absolutePath)
            return null
        }

        LOGGER.debug("Loading data type {} from {}", dataType, configFile.absolutePath)
        return configFile.bufferedReader()
    }

    fun getConfigContent(dataType: DataType, dataConfig: DataConfig): String =
        getConfigContentReader(dataType, dataConfig)?.readText() ?: ""

    fun getConfigContentLines(dataType: DataType, dataConfig: DataConfig): List<String> =
        getConfigContentReader(dataType, dataConfig)?.readLines() ?: emptyList()

    fun getCertificateConfigContent(dataConfig: DataConfig): String = getConfigContent(DataType.CERTIFICATE_CONFIG, dataConfig)
    fun getLibraryDefinitionContent(dataConfig: DataConfig): Set<String> = getConfigContentLines(DataType.LIBRARY_DEFINITION, dataConfig).toSet()
    fun getLibraryInformationContent(dataConfig: DataConfig): Set<String> = getConfigContentLines(DataType.LIBRARY_INFORMATION, dataConfig).toSet()
    fun getManifestConfigContent(dataConfig: DataConfig): String = getConfigContent(DataType.MANIFEST_CONFIG, dataConfig)
}

enum class DataType {
    CERTIFICATE_CONFIG,
    LIBRARY_DEFINITION,
    LIBRARY_INFORMATION,
    MANIFEST_CONFIG,
}
