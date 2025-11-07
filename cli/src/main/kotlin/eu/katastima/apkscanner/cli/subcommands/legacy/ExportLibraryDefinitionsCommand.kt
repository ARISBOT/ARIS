/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.cli.subcommands.legacy

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import eu.katastima.apkscanner.config.ApkScannerConfig
import eu.katastima.apkscanner.database.DatabaseUtil
import eu.katastima.apkscanner.utils.LegacyUtil
import java.io.File

class ExportLibraryDefinitionsCommand : CliktCommand("export-library-definitions") {

    override fun help(context: Context): String = """
        Exports library definitions to the specified library paths in the legacy config section. The exported files will have ".exported" as suffix.
        """.trimIndent()

    override fun run() {
        val apkScannerConfig = ApkScannerConfig.getConfig()

        val database = DatabaseUtil.getDatabase()
        DatabaseUtil.setupDatabase(database, apkScannerConfig.databaseConfig)

        val legacyConfig = apkScannerConfig.legacyConfig
        if (File(legacyConfig.libraryInformationPath).exists() && File(legacyConfig.libraryDefinitionPath).exists()) {
            LegacyUtil.exportLibraryDefinitions(database, apkScannerConfig)
        }
    }
}
