/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.cli

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.requireObject
import org.katastima.apkscanner.cli.configs.CliConfig

abstract class ApkScannerCommand(name: String? = null) : SuspendingCliktCommand(name) {

    val cliConfig by requireObject<CliConfig>()

    fun silenceableEcho() {
        silenceableEcho("")
    }

    fun silenceableEcho(
        message: Any?,
        trailingNewline: Boolean = true,
        err: Boolean = false,
    ) {
        if (cliConfig.quiet.not()) {
            currentContext.echoMessage(currentContext, message, trailingNewline, err)
        }
    }
}
