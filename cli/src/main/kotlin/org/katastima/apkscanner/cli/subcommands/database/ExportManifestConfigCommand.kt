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
import org.katastima.apkscanner.internal.RepositoryUtil
import java.io.File

class ExportManifestConfigCommand : ApkScannerCommand("export-manifest-config") {

    override fun help(context: Context): String = """
        Exports manifest config to the specified export paths in the data config section.
        """.trimIndent()

    private val outputFile: File? by option("--output", "-o")
        .file(canBeFile = false)
        .help("Write manifest config export to the given path instead of using the path specified in the data config section.")

    override suspend fun run() {
        val apkScannerConfig = cliConfig.apkScannerConfig

        val manifestRepository = RepositoryUtil.getManifestRepository(apkScannerConfig)
        val exportPath = outputFile?.absolutePath ?: apkScannerConfig.dataConfig.manifestConfigExportPath
        val exportedManifestConfig = ExportUtil.exportManifestConfig(manifestRepository, exportPath)
        silenceableEcho("Exported manifest config (${exportedManifestConfig.getGroupAndCountString()}) to: $exportPath")
    }
}
