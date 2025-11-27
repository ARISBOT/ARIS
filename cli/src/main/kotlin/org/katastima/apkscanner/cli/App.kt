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
import org.katastima.apkscanner.cli.subcommands.database.CreateDatabaseCommand
import org.katastima.apkscanner.cli.subcommands.database.DatabaseCommand
import org.katastima.apkscanner.cli.subcommands.database.ExportAllCommand
import org.katastima.apkscanner.cli.subcommands.database.ExportLibraryDefinitionsCommand
import org.katastima.apkscanner.cli.subcommands.database.ExportManifestConfigCommand
import org.katastima.apkscanner.cli.subcommands.database.ExportSigningCertificateDataCommand
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
                ExportAllCommand(),
                ExportLibraryDefinitionsCommand(),
                ExportManifestConfigCommand(),
                ExportSigningCertificateDataCommand(),
            ),
            ScanAPKCommand(),
        )
        .main(args)
}
