/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.cli.subcommands.legacy

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.Context
import eu.katastima.apkscanner.config.ApkScannerConfig
import eu.katastima.apkscanner.database.DatabaseUtil
import eu.katastima.apkscanner.utils.LegacyUtil
import java.io.File

class ExportLibraryDefinitionsCommand : SuspendingCliktCommand("export-library-definitions") {

    override fun help(context: Context): String = """
        Exports library definitions to the specified library paths in the legacy config section. The exported files will have ".exported" as suffix.
        """.trimIndent()

    override suspend fun run() {
        val apkScannerConfig = ApkScannerConfig.getConfig()

        val database = DatabaseUtil.getDatabase()
        DatabaseUtil.setupDatabase(database, apkScannerConfig.databaseConfig)

        val legacyConfig = apkScannerConfig.legacyConfig
        val legacyInformationFile = File(legacyConfig.libraryInformationPath)
        val legacyDefinitionFile = File(legacyConfig.libraryDefinitionPath)
        if (legacyInformationFile.exists() && legacyDefinitionFile.exists()) {
            val exportedData = LegacyUtil.exportLibraryDefinitions(database, apkScannerConfig)
            echo("Exported ${exportedData.second.size} library definitions to: ${legacyDefinitionFile.absolutePath}.exported")
            echo("Exported ${exportedData.first.size} library information entries to: ${legacyInformationFile.absolutePath}.exported")
        }
    }
}
