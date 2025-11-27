/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.cli.subcommands.database

import com.github.ajalt.clikt.core.Context
import org.katastima.apkscanner.cli.ApkScannerCommand
import org.katastima.apkscanner.data.ExportUtil
import org.katastima.apkscanner.database.DatabaseUtil
import java.io.File

class ExportLibraryDefinitionsCommand : ApkScannerCommand("export-library-definitions") {

    override fun help(context: Context): String = """
        Exports library definitions to the specified library paths in the legacy config section. The exported files will have ".exported" as suffix.
        """.trimIndent()

    override suspend fun run() {
        val apkScannerConfig = cliConfig.apkScannerConfig

        val database = DatabaseUtil.getDatabase(apkScannerConfig.databaseConfig)
        DatabaseUtil.setupDatabase(database, apkScannerConfig.databaseConfig.debug)

        val dataConfig = apkScannerConfig.dataConfig
        val informationFile = File(dataConfig.libraryInformationPath)
        val definitionFile = File(dataConfig.libraryDefinitionPath)
        if (informationFile.exists() && definitionFile.exists()) {
            val exportedData = ExportUtil.exportLibraryData(database, apkScannerConfig)
            silenceableEcho("Exported ${exportedData.second.size} library definitions to: ${informationFile.absolutePath}.exported")
            silenceableEcho("Exported ${exportedData.first.size} library information entries to: ${definitionFile.absolutePath}.exported")
        }
    }
}
