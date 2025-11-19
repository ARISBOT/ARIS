/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.cli.subcommands.config

import com.github.ajalt.clikt.core.Context
import org.katastima.apkscanner.cli.ApkScannerCommand
import java.io.File

class ShowConfigCommand : ApkScannerCommand("show") {

    override fun help(context: Context): String = "Show the current application configuration"

    override suspend fun run() {
        echo("${cliConfig.configFilePath}:")
        echo(cliConfig.toString())
        echo()

        val configFile = File(cliConfig.apkScannerConfigFilePath)
        echo("${configFile.absolutePath}:")
        echo(cliConfig.apkScannerConfig.toString())
    }
}
