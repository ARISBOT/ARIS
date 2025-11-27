/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.cli.subcommands.database

import com.github.ajalt.clikt.core.Context
import org.katastima.apkscanner.cli.ApkScannerCommand
import org.katastima.apkscanner.data.ExportUtil

class ExportAllCommand : ApkScannerCommand("export-all") {

    override fun help(context: Context): String = """
        Exports everything that can be exported to the specified export paths in the data config section.
        """.trimIndent()

    override suspend fun run() {
        val apkScannerConfig = cliConfig.apkScannerConfig

        val exportResult = ExportUtil.exportAll(apkScannerConfig)
        silenceableEcho(exportResult)
    }
}
