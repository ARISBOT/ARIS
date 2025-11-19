/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.cli.subcommands.config

import com.github.ajalt.clikt.core.Context
import org.katastima.apkscanner.cli.ApkScannerCommand

class ShowConfigCommand : ApkScannerCommand("show") {

    override fun help(context: Context): String = "Show the current application configuration"

    override suspend fun run() {
        val apkScannerConfig = cliConfig.apkScannerConfig
        echo(apkScannerConfig.toString())
    }
}
