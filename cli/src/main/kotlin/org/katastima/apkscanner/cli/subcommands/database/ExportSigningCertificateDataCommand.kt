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
import org.katastima.apkscanner.internal.RepositoryUtil
import java.io.File

class ExportSigningCertificateDataCommand : ApkScannerCommand("export-signing-certificate-data") {

    override fun help(context: Context): String = """
        Exports signing certificate data to the specified export paths in the data config section.
        """.trimIndent()

    private val outputFile: File? by option("--output", "-o")
        .file(canBeFile = false)
        .help("Write signing certificate data export to the given path instead of using the path specified in the data config section.")

    override suspend fun run() {
        val apkScannerConfig = cliConfig.apkScannerConfig

        val database = DatabaseUtil.getDatabase(apkScannerConfig.databaseConfig)
        DatabaseUtil.setupDatabase(database, apkScannerConfig.databaseConfig.debug)

        val denylistExportPath = outputFile?.absolutePath ?: apkScannerConfig.dataConfig.certificateDenylistExportPath
        val exportedListData = ExportUtil.exportCertificateDenylist(
            RepositoryUtil.getCertificateRepository(database, apkScannerConfig),
            denylistExportPath,
        )
        silenceableEcho("Exported ${exportedListData.size} denied signing certificates to: $denylistExportPath")
    }
}
