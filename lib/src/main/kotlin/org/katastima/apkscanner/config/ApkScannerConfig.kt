/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.config

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okio.source
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

        private var apkScannerConfig: ApkScannerConfig? = null

        fun getConfig(): ApkScannerConfig {
            if (apkScannerConfig == null) {
                apkScannerConfig = if (doesConfigExist()) {
                    try {
                        Yaml.default.decodeFromSource(serializer(), getConfigFile().source())
                    } catch (exc: Exception) {
                        exc.printStackTrace()
                        ApkScannerConfig()
                    }
                } else {
                    ApkScannerConfig()
                }
            }
            return apkScannerConfig!!
        }

        fun getConfigFile(): File = File(CONFIG_NAME)

        fun doesConfigExist(): Boolean = getConfigFile().exists()
    }
}
