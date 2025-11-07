/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.cli.subcommands.config

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import eu.katastima.apkscanner.config.ApkScannerConfig

class ShowConfigCommand : CliktCommand("show") {

    override fun help(context: Context): String = "Show the current application configuration"

    override fun run() {
        echo("Configuration file: ${ApkScannerConfig.getConfigFile().absolutePath}")
        if (!ApkScannerConfig.doesConfigExist()) {
            echo("  - does not exist, create to overwrite values")
        }
        echo()

        val apkScannerConfig: ApkScannerConfig = ApkScannerConfig.getConfig()
        echo(apkScannerConfig.toString())
    }
}
