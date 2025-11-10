/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.cli.subcommands.config

import com.github.ajalt.clikt.command.SuspendingNoOpCliktCommand
import com.github.ajalt.clikt.core.Context

class ConfigCommand : SuspendingNoOpCliktCommand() {

    override fun help(context: Context): String = "Interact with the application configuration."
}
