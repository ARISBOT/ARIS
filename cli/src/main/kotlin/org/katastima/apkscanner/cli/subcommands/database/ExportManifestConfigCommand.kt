/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.cli.subcommands.database

import com.github.ajalt.clikt.core.Context
import org.katastima.apkscanner.cli.ApkScannerCommand
import org.katastima.apkscanner.data.ExportUtil
import org.katastima.apkscanner.database.DatabaseUtil
import org.katastima.apkscanner.internal.RepositoryUtil

class ExportManifestConfigCommand : ApkScannerCommand("export-manifest-config") {

    override fun help(context: Context): String = """
        Exports manifest config to the specified export paths in the data config section.
        """.trimIndent()

    override suspend fun run() {
        val apkScannerConfig = cliConfig.apkScannerConfig

        val database = DatabaseUtil.getDatabase(apkScannerConfig.databaseConfig)
        DatabaseUtil.setupDatabase(database, apkScannerConfig.databaseConfig.debug)

        val manifestRepository = RepositoryUtil.getManifestRepository(database, apkScannerConfig)
        val exportPath = apkScannerConfig.dataConfig.manifestConfigExportPath
        val exportedManifestConfig = ExportUtil.exportManifestConfig(manifestRepository, exportPath)
        silenceableEcho("Exported manifest config (${exportedManifestConfig.getGroupAndCountString()}) to: $exportPath")
    }
}
