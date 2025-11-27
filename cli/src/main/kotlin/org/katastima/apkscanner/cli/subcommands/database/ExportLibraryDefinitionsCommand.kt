/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.cli.subcommands.database

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import org.katastima.apkscanner.cli.ApkScannerCommand
import org.katastima.apkscanner.data.ExportUtil
import org.katastima.apkscanner.database.DatabaseUtil
import java.io.File

class ExportLibraryDefinitionsCommand : ApkScannerCommand("export-library-definitions") {

    override fun help(context: Context): String = """
        Exports library definitions to the specified library export paths in the data config section.
        """.trimIndent()

    private val outputDefinitionFile: File? by option("--output-definition")
        .file(canBeFile = false)
        .help("Write library definition export to the given path instead of using the path specified in the data config section.")

    private val outputInfoFile: File? by option("--output-info")
        .file(canBeFile = false)
        .help("Write library information export to the given path instead of using the path specified in the data config section.")

    override suspend fun run() {
        val apkScannerConfig = cliConfig.apkScannerConfig

        val database = DatabaseUtil.getDatabase(apkScannerConfig.databaseConfig)
        DatabaseUtil.setupDatabase(database, apkScannerConfig.databaseConfig.debug)

        val definitionExportPath = outputDefinitionFile?.absolutePath ?: apkScannerConfig.dataConfig.libraryDefinitionExportPath
        val informationExportPath = outputInfoFile?.absolutePath ?: apkScannerConfig.dataConfig.libraryInformationExportPath

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
