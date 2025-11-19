/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.cli.utils

import org.katastima.apkscanner.cli.configs.ConsoleOutputConfig

object CliUtil {

    private const val FORMAT_END = "\u001B[0m"

    private const val FORMAT_BOLD = "\u001B[1m"
    private const val FORMAT_COLOR_GREEN = "\u001B[92m"
    private const val FORMAT_COLOR_RED = "\u001B[91m"
    private const val FORMAT_COLOR_YELLOW = "\u001B[93m"

    private fun format(consoleOutputConfig: ConsoleOutputConfig, message: String, format: String): String = buildString {
        if (consoleOutputConfig.richOutputEnabled) {
            append(format)
        }
        append(message)
        if (consoleOutputConfig.richOutputEnabled) {
            append(FORMAT_END)
        }
    }

    fun formatBold(consoleOutputConfig: ConsoleOutputConfig, message: String): String = format(consoleOutputConfig, message, FORMAT_BOLD)
    fun formatGreen(consoleOutputConfig: ConsoleOutputConfig, message: String): String = format(consoleOutputConfig, message, FORMAT_COLOR_GREEN)
    fun formatRed(consoleOutputConfig: ConsoleOutputConfig, message: String): String = format(consoleOutputConfig, message, FORMAT_COLOR_RED)
    fun formatYellow(consoleOutputConfig: ConsoleOutputConfig, message: String): String = format(consoleOutputConfig, message, FORMAT_COLOR_YELLOW)
}
