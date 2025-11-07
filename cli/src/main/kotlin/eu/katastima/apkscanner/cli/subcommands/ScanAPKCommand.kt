/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.cli.subcommands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import eu.katastima.apkscanner.scanapk.ScanAPK
import java.io.File

class ScanAPKCommand : CliktCommand() {

    private val apkFiles: List<File> by argument("apk")
        .file(mustExist = true, mustBeReadable = true, canBeDir = false)
        .help("A single or multiple APK files which should get scanned")
        .multiple(true)

    private val libraryDefinitionsFile: File by option("--library-definitions")
        .file(mustExist = true, mustBeReadable = true, canBeDir = false)
        .default(File("./libsmali.jsonl"))
        .help("A jsonl file containing smali definitions")

    private val libraryInformationFile: File by option("--library-information")
        .file(mustExist = true, mustBeReadable = true, canBeDir = false)
        .default(File("./libinfo.jsonl"))
        .help("A jsonl file containing library information")

    init {
        context {
            helpFormatter = { MordantHelpFormatter(it, showDefaultValues = true, requiredOptionMarker = "*") }
        }
    }

    override fun help(context: Context): String = "Scan a single apk and list its used libraries, offending libraries and anti features."

    override fun run() {
        if (!libraryDefinitionsFile.exists()) {
            echoFormattedHelp(CliktError("The provided library definition file (${libraryDefinitionsFile.absolutePath}) does not exist!"))
            throw ProgramResult(1)
        }

        if (!libraryInformationFile.exists()) {

            echoFormattedHelp(CliktError("The provided library information file (${libraryInformationFile.absolutePath}) does not exist!"))
            throw ProgramResult(1)
        }

        ScanAPK(libraryDefinitionsFile, libraryInformationFile).use {
            echo("Scanning ${apkFiles.size} APK(s).")
            it.scanMulti(apkFiles)
        }
    }
}
