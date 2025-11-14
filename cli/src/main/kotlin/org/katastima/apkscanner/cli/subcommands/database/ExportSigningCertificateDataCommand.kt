/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.cli.subcommands.database

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.Context
import org.katastima.apkscanner.config.ApkScannerConfig
import org.katastima.apkscanner.database.CertificateDataUtil
import org.katastima.apkscanner.database.DatabaseUtil
import java.io.File

class ExportSigningCertificateDataCommand : SuspendingCliktCommand("export-signing-certificate-data") {

    override fun help(context: Context): String = """
        Exports signing certificate data to the specified paths in the data config section. The exported files will have ".exported" as suffix.
        """.trimIndent()

    override suspend fun run() {
        val apkScannerConfig = ApkScannerConfig.getConfig()
        val dataConfig = apkScannerConfig.dataConfig

        val database = DatabaseUtil.getDatabase(apkScannerConfig.databaseConfig)
        DatabaseUtil.setupDatabase(database, apkScannerConfig.databaseConfig.debug)

        val denylistPath = File(dataConfig.certificateDenylistPath)
        if (denylistPath.exists()) {
            val exportedListData = CertificateDataUtil.exportCertificateDenylist(database, apkScannerConfig)
            echo("Exported ${exportedListData.size} denied signing certificates to: ${denylistPath.absolutePath}.exported")
        }
    }
}
