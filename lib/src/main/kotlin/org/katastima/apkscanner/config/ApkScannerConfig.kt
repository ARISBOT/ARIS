/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.config

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okio.source
import org.slf4j.LoggerFactory
import java.io.File

@Serializable
data class ApkScannerConfig(
    @SerialName("data") val dataConfig: DataConfig = DataConfig(),
    @SerialName("database") val databaseConfig: DatabaseConfig = DatabaseConfig(),
    @SerialName("scan") val scanConfig: ScanConfig = ScanConfig(),
) {

    override fun toString(): String {
        return Yaml.default.encodeToString(serializer(), this)
    }

    companion object {

        private const val CONFIG_NAME = "apkscanner.yaml"

        private val LOGGER = LoggerFactory.getLogger(ApkScannerConfig::class.java)

        private var apkScannerConfig: ApkScannerConfig? = null

        fun getConfig(configFile: File? = null): ApkScannerConfig {
            if (apkScannerConfig == null) {
                val configFile = getConfigFile(configFile)
                apkScannerConfig = if (configFile.exists()) {
                    try {
                        val yamlConfiguration = YamlConfiguration(encodeDefaults = true, strictMode = false)
                        val yaml = Yaml(configuration = yamlConfiguration)
                        yaml.decodeFromSource(serializer(), configFile.source())
                    } catch (exc: Exception) {
                        LOGGER.warn("Could not load apk scanner config (${configFile.absolutePath})", exc)
                        ApkScannerConfig()
                    }
                } else {
                    ApkScannerConfig()
                }
            }
            return apkScannerConfig!!
        }

        private fun getConfigFile(configFile: File?): File = configFile ?: File(CONFIG_NAME)
    }
}
