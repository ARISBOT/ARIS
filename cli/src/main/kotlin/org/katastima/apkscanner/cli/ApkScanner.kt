/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.cli

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.findOrSetObject
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.nullableFlag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import org.katastima.apkscanner.cli.configs.CliConfig
import java.io.File

class ApkScanner : SuspendingCliktCommand() {

    private val configFile: File? by option("-c", "--config")
        .file(canBeDir = false)
        .help("Specify a config file to use")
        .default(File("apkscanner-cli.yaml"))

    private val verbose: Boolean? by option("-v", "--verbose")
        .nullableFlag("--no-verbose")
        .help("Print additional verbose output; ignored when -q/--quiet is enabled.")

    private val quiet: Boolean? by option("-q", "--quiet")
        .nullableFlag("--no-quiet")
        .help("Do not print any program output (where possible)")

    private val cliConfig: CliConfig by findOrSetObject { CliConfig() }

    override suspend fun run() {

        val parsedCliConfig: CliConfig? = CliConfig.getConfigFromFile(configFile)

        // Set verbose first, as we are eager to print!
        cliConfig.verbose = if (verbose != null) {
            verbose == true
        } else {
            parsedCliConfig?.verbose ?: false
        }

        val parsedConfigFile = configFile
        if (parsedConfigFile != null) {
            cliConfig.configFilePath = parsedConfigFile.absolutePath
            if (cliConfig.verbose) {
                echo("Loaded cli config from: ${cliConfig.configFilePath}")
                echo()
            }
        }

        cliConfig.quiet = if (quiet != null) {
            quiet == true
        } else {
            parsedCliConfig?.quiet ?: false
        }

        parsedCliConfig?.apkScannerConfig?.let { cliConfig.apkScannerConfig = it }
        parsedCliConfig?.consoleOutputConfig?.let { cliConfig.consoleOutputConfig = it }
        parsedCliConfig?.scanApkConfig?.let { cliConfig.scanApkConfig = it }
    }
}
