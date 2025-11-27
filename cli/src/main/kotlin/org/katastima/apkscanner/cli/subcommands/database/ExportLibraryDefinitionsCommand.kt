/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.cli.subcommands.database

import com.github.ajalt.clikt.core.Context
import org.katastima.apkscanner.cli.ApkScannerCommand
import org.katastima.apkscanner.data.ExportUtil
import org.katastima.apkscanner.database.DatabaseUtil

class ExportLibraryDefinitionsCommand : ApkScannerCommand("export-library-definitions") {

    override fun help(context: Context): String = """
        Exports library definitions to the specified library export paths in the data config section.
        """.trimIndent()

    override suspend fun run() {
        val apkScannerConfig = cliConfig.apkScannerConfig

        val database = DatabaseUtil.getDatabase(apkScannerConfig.databaseConfig)
        DatabaseUtil.setupDatabase(database, apkScannerConfig.databaseConfig.debug)

        val definitionExportPath = apkScannerConfig.dataConfig.libraryDefinitionExportPath
        val informationExportPath = apkScannerConfig.dataConfig.libraryInformationExportPath

        val exportedData = ExportUtil.exportLibraryData(
            database,
            definitionExportPath,
            informationExportPath,
            apkScannerConfig.databaseConfig.debug,
        )

        silenceableEcho("Exported ${exportedData.second.size} library definitions to: $definitionExportPath")
        silenceableEcho("Exported ${exportedData.first.size} library information entries to: $informationExportPath")
    }
}
