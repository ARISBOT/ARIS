/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.config

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okio.source
import java.io.File

@Serializable
data class ApkScannerConfig(
    @SerialName("database") val databaseConfig: DatabaseConfig,
) {

    override fun toString(): String {
        return Yaml.default.encodeToString(serializer(), this)
    }

    companion object {

        private const val CONFIG_NAME = "apkscanner.yaml"

        private var apkScannerConfig: ApkScannerConfig? = null

        fun getConfig(): ApkScannerConfig {
            if (apkScannerConfig == null) {
                apkScannerConfig = if (doesConfigExist()) {
                    Yaml.default.decodeFromSource(serializer(), getConfigFile().source())
                } else {
                    ApkScannerConfig(DatabaseConfig())
                }
            }
            return apkScannerConfig!!
        }

        fun getConfigFile(): File = File(CONFIG_NAME)

        fun doesConfigExist(): Boolean = getConfigFile().exists()
    }
}
