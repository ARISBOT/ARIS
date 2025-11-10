/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.cli.subcommands

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.types.file
import eu.katastima.apkscanner.models.LibraryInformation
import eu.katastima.apkscanner.scanapk.ApkScanResult
import eu.katastima.apkscanner.scanapk.ScanAPK
import java.io.File

class ScanAPKCommand : SuspendingCliktCommand() {

    private val apkFiles: List<File> by argument("apk")
        .file(mustExist = true, mustBeReadable = true, canBeDir = false)
        .help("A single or multiple APK files which should get scanned")
        .multiple(true)

    override val printHelpOnEmptyArgs = true

    private var hasOffendingLibrary: Boolean = false

    init {
        context {
            helpFormatter = { MordantHelpFormatter(it, showDefaultValues = true, requiredOptionMarker = "*") }
        }
    }

    override fun help(context: Context): String = "Scan a single apk and list its used libraries, offending libraries and anti features."

    override suspend fun run() {
        ScanAPK().use {
            echo("Scanning ${apkFiles.size} APK(s).\n")
            it.scanMulti(apkFiles, this::printScanResult)
        }
        echo("Have a nice day!")

        if (hasOffendingLibrary) {
            throw ProgramResult(1)
        }
    }

    private fun printScanResult(apkFile: File, scanResult: ApkScanResult) {
        echo("Scanned APK: ${apkFile.absolutePath}\n")

        // Store offending libraries in an own list to prevent having to iterate through the whole list multiple times.
        val offendingLibraries: MutableList<LibraryInformation> = mutableListOf()

        echo("Libraries detected:")
        echo("-------------------")
        scanResult.detectedLibraries.forEach { library ->
            echo("* ${library.name} (${library.libraryId}): ${library.type}, ${library.license}", trailingNewline = false)

            if (library.antiFeatures.isNotEmpty()) {
                echo("; ${formatAntiFeatures(library.antiFeatures)}", trailingNewline = false)
                offendingLibraries.add(library)
            }
            echo()
        }
        echo()

        if (offendingLibraries.isEmpty()) {
            echo("No offending libraries found.")
        } else {
            hasOffendingLibrary = true

            echo("Offending libraries:")
            echo("--------------------")
            offendingLibraries.forEach { offendingLibrary ->
                echo("* ${offendingLibrary.name} (${offendingLibrary.libraryId}): ${formatAntiFeatures(offendingLibrary.antiFeatures)}")
            }
            echo()

            echo("${offendingLibraries.size} offending ${if (offendingLibraries.size == 1) "library" else "libraries"} found.")
        }
        echo()
    }

    private fun formatAntiFeatures(antiFeatures: Array<String>): String = buildString {
        // TODO: configurable console output formatting.
        append("\u001B[1m")
        val antiFeatureIterator = antiFeatures.iterator()
        while (antiFeatureIterator.hasNext()) {
            append(antiFeatureIterator.next())
            if (antiFeatureIterator.hasNext()) {
                append(",")
            }
        }
        // TODO: configurable console output formatting.
        append("\u001B[0m")
    }
}
