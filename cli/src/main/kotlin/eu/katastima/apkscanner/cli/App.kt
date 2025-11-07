/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.cli

import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import eu.katastima.apkscanner.cli.subcommands.ScanAPKCommand
import eu.katastima.apkscanner.cli.subcommands.config.ConfigCommand
import eu.katastima.apkscanner.cli.subcommands.config.ShowConfigCommand
import eu.katastima.apkscanner.cli.subcommands.database.CreateDatabaseCommand
import eu.katastima.apkscanner.cli.subcommands.database.DatabaseCommand
import eu.katastima.apkscanner.cli.subcommands.legacy.ExportLibraryDefinitionsCommand
import eu.katastima.apkscanner.cli.subcommands.legacy.LegacyCommand

fun main(args: Array<String>) = ApkScanner()
    .subcommands(
        ConfigCommand().subcommands(
            ShowConfigCommand(),
        ),
        DatabaseCommand().subcommands(
            CreateDatabaseCommand(),
        ),
        LegacyCommand().subcommands(
            ExportLibraryDefinitionsCommand(),
        ),
        ScanAPKCommand(),
    )
    .main(args)
