/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.cli

import eu.katastima.apkscanner.Printer

fun main() {
    val name = "Kotlin"
    val message = "Hello, $name!"
    val printer = Printer(message)
    printer.printMessage()

    for (i in 1..5) {
        println("i = $i")
    }
}
