/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.types.int
import eu.katastima.apkscanner.Printer

class ApkScanner : CliktCommand() {
    val count: Int by option().int().default(3).help("How far to count")
    val name: String by option().prompt("Enter your name").help("The person to greet")

    override fun run() {
        val message = "Hello, $name!"
        val printer = Printer(message)
        printer.printMessage()

        for (i in 1..count) {
            println("i = $i")
        }
    }
}
