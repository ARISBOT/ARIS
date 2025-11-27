/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.cli.subcommands.database

import com.github.ajalt.clikt.core.Context
import org.katastima.apkscanner.cli.ApkScannerCommand
import org.katastima.apkscanner.data.ExportUtil
import org.katastima.apkscanner.database.DatabaseUtil

class ExportSigningCertificateDataCommand : ApkScannerCommand("export-signing-certificate-data") {

    override fun help(context: Context): String = """
        Exports signing certificate data to the specified export paths in the data config section.
        """.trimIndent()

    override suspend fun run() {
        val apkScannerConfig = cliConfig.apkScannerConfig

        val database = DatabaseUtil.getDatabase(apkScannerConfig.databaseConfig)
        DatabaseUtil.setupDatabase(database, apkScannerConfig.databaseConfig.debug)

        val denylistExportPath = apkScannerConfig.dataConfig.certificateDenylistExportPath
        val exportedListData = ExportUtil.exportCertificateDenylist(database, denylistExportPath)
        silenceableEcho("Exported ${exportedListData.size} denied signing certificates to: $denylistExportPath")
    }
}
