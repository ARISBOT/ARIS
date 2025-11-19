/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.cli.configs

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import okio.source
import org.katastima.apkscanner.config.ApkScannerConfig
import org.slf4j.LoggerFactory
import java.io.File

@Serializable
data class CliConfig(
    var verbose: Boolean = false,
    var quiet: Boolean = false,
    @SerialName("apkscanner_config_file_path") val apkScannerConfigFilePath: String = "apkscanner.yaml",
    @SerialName("console_output") val consoleOutputConfig: ConsoleOutputConfig = ConsoleOutputConfig(),
    @Transient var configFilePath: String = "apkscanner-cli.yaml",
    @Transient var apkScannerConfig: ApkScannerConfig = ApkScannerConfig.getConfig(File(apkScannerConfigFilePath)),
) {

    override fun toString(): String {
        return yaml.encodeToString(serializer(), this)
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(CliConfig::class.java)

        private val yaml: Yaml by lazy {
            val yamlConfiguration = YamlConfiguration(encodeDefaults = true, strictMode = false)
            Yaml(configuration = yamlConfiguration)
        }

        fun getConfigFromFile(configFile: File?): CliConfig? {
            if (configFile == null || !configFile.exists()) {
                return null
            }

            return try {
                yaml.decodeFromSource(serializer(), configFile.source())
            } catch (exc: Exception) {
                LOGGER.error("Could not load config (${configFile.absolutePath})", exc)
                null
            }
        }
    }
}
