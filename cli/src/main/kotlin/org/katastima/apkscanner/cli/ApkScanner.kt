/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.cli

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.findOrSetObject
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import org.katastima.apkscanner.config.ApkScannerConfig

class ApkScanner : SuspendingCliktCommand() {

    private val quiet: Boolean by option("--quiet", "-q")
        .flag()
        .help("Do not print any program output (where possible)")

    private val config by findOrSetObject { CliConfig() }

    override suspend fun run() {
        config.quiet = quiet
        config.apkScannerConfig = ApkScannerConfig.getConfig()
    }
}
