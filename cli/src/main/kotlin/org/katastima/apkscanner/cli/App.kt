/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.cli

import com.github.ajalt.clikt.command.main
import com.github.ajalt.clikt.completion.completionOption
import com.github.ajalt.clikt.core.subcommands
import org.katastima.apkscanner.cli.subcommands.ScanAPKCommand
import org.katastima.apkscanner.cli.subcommands.config.ConfigCommand
import org.katastima.apkscanner.cli.subcommands.config.ShowConfigCommand
import org.katastima.apkscanner.cli.subcommands.database.*
import java.util.logging.Level
import java.util.logging.Logger

suspend fun main(args: Array<String>) {
    // Disable logging for apktool.
    Logger.getLogger("brut.androlib").apply {
        level = Level.WARNING
    }

    return ApkScanner()
        .completionOption()
        .subcommands(
            ConfigCommand().subcommands(
                ShowConfigCommand(),
            ),
            DatabaseCommand().subcommands(
                CreateDatabaseCommand(),
                ExportLibraryDefinitionsCommand(),
                ExportManifestConfigCommand(),
                ExportSigningCertificateDataCommand(),
            ),
            ScanAPKCommand(),
        )
        .main(args)
}
