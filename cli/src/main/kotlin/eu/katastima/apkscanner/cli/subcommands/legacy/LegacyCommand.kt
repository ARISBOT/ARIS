/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.cli.subcommands.legacy

import com.github.ajalt.clikt.command.SuspendingNoOpCliktCommand
import com.github.ajalt.clikt.core.Context

class LegacyCommand : SuspendingNoOpCliktCommand() {

    override fun help(context: Context): String = "Collection of legacy features to provide a migration path."
}
