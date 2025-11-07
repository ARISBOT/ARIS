/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.cli

import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import eu.katastima.apkscanner.cli.subcommands.ScanAPKCommand

fun main(args: Array<String>) = ApkScanner()
    .subcommands(
        ScanAPKCommand(),
    )
    .main(args)
