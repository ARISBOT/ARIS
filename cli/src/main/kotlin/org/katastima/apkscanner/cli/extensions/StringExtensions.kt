/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.cli.extensions

import org.katastima.apkscanner.cli.configs.ConsoleOutputConfig
import org.katastima.apkscanner.cli.utils.CliUtil

fun String.formatBold(consoleOutputConfig: ConsoleOutputConfig): String = CliUtil.formatBold(consoleOutputConfig, this)
fun String.formatGreen(consoleOutputConfig: ConsoleOutputConfig): String = CliUtil.formatGreen(consoleOutputConfig, this)
fun String.formatRed(consoleOutputConfig: ConsoleOutputConfig): String = CliUtil.formatRed(consoleOutputConfig, this)
fun String.formatYellow(consoleOutputConfig: ConsoleOutputConfig): String = CliUtil.formatYellow(consoleOutputConfig, this)
