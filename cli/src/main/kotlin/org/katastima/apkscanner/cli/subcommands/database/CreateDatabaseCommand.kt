/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.cli.subcommands.database

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.katastima.apkscanner.cli.ApkScannerCommand
import org.katastima.apkscanner.data.ImportUtil
import org.katastima.apkscanner.database.DatabaseUtil
import kotlin.system.measureTimeMillis

class CreateDatabaseCommand : ApkScannerCommand("setup") {

    val forceSetup by option("--force", "-f")
        .flag()
        .help("Force database setup without waiting when data already exists")

    override fun help(context: Context): String = "Setup the database for usage, removing existing data."

    override suspend fun run() {
        val apkScannerConfig = cliConfig.apkScannerConfig
        val database = DatabaseUtil.getDatabase(apkScannerConfig.databaseConfig)

        if (!forceSetup) {
            val tablesExist = transaction(database) {
                SchemaUtils.listTables().isNotEmpty()
            }
            if (tablesExist) {
                echo("WARNING: existing data found, which will be destroyed!")
                echo("Terminate the application, if this was a mistake!")
                echo("Waiting for 30 seconds (this can be skipped with using the force option):")

                with(Dispatchers.Default) {
                    var counter = 30
                    echo("$counter", trailingNewline = false)
                    while (counter > 0) {
                        delay(1_000L)
                        counter--
                        echo(" $counter", trailingNewline = false)
                    }
                    echo()
                }
                echo()
            }
        }

        val setupTime = measureTimeMillis {
            DatabaseUtil.dropTables(database, apkScannerConfig.databaseConfig.debug)
            DatabaseUtil.setupDatabase(database, apkScannerConfig.databaseConfig.debug)

            // Import all the data.
            ImportUtil.importAll(database, apkScannerConfig)
        }

        silenceableEcho("Database set up in $setupTime ms")
    }
}
