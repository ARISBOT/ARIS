/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.cli.subcommands.database

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.Context
import eu.katastima.apkscanner.config.ApkScannerConfig
import eu.katastima.apkscanner.database.DatabaseUtil
import eu.katastima.apkscanner.database.ManifestDataUtil
import java.io.File

class ExportManifestConfigCommand : SuspendingCliktCommand("export-manifest-config") {

    override fun help(context: Context): String = """
        Exports manifest config to the specified paths in the data config section. The exported files will have ".exported" as suffix.
        """.trimIndent()

    override suspend fun run() {
        val apkScannerConfig = ApkScannerConfig.getConfig()
        val dataConfig = apkScannerConfig.dataConfig

        val database = DatabaseUtil.getDatabase()
        DatabaseUtil.setupDatabase(database, apkScannerConfig.databaseConfig)

        val manifestConfigPath = File(dataConfig.manifestConfigPath)
        if (manifestConfigPath.exists()) {
            val exportedManifestConfig = ManifestDataUtil.exportManifestConfig(database, apkScannerConfig)
            echo("Exported manifest config (${exportedManifestConfig.getGroupAndCountString()}) to: ${manifestConfigPath.absolutePath}.exported")
        }
    }
}
