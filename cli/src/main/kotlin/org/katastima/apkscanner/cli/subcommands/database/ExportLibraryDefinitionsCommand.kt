/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.cli.subcommands.database

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.Context
import org.katastima.apkscanner.config.ApkScannerConfig
import org.katastima.apkscanner.database.DatabaseUtil
import org.katastima.apkscanner.database.LibraryDataUtil
import java.io.File

class ExportLibraryDefinitionsCommand : SuspendingCliktCommand("export-library-definitions") {

    override fun help(context: Context): String = """
        Exports library definitions to the specified library paths in the legacy config section. The exported files will have ".exported" as suffix.
        """.trimIndent()

    override suspend fun run() {
        val apkScannerConfig = ApkScannerConfig.getConfig()

        val database = DatabaseUtil.getDatabase(apkScannerConfig.databaseConfig)
        DatabaseUtil.setupDatabase(database, apkScannerConfig.databaseConfig.debug)

        val dataConfig = apkScannerConfig.dataConfig
        val informationFile = File(dataConfig.libraryInformationPath)
        val definitionFile = File(dataConfig.libraryDefinitionPath)
        if (informationFile.exists() && definitionFile.exists()) {
            val exportedData = LibraryDataUtil.exportLibraryDefinitions(database, apkScannerConfig)
            echo("Exported ${exportedData.second.size} library definitions to: ${informationFile.absolutePath}.exported")
            echo("Exported ${exportedData.first.size} library information entries to: ${definitionFile.absolutePath}.exported")
        }
    }
}
