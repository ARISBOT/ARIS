/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.cli.subcommands.database

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import eu.katastima.apkscanner.config.ApkScannerConfig
import eu.katastima.apkscanner.database.CertificateDataUtil
import eu.katastima.apkscanner.database.DatabaseUtil
import eu.katastima.apkscanner.database.LibraryDataUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class CreateDatabaseCommand : SuspendingCliktCommand("setup") {

    val forceSetup by option("--force", "-f")
        .flag()
        .help("Force database setup without waiting when data already exists")

    override fun help(context: Context): String = "Setup the database for usage, removing existing data."

    override suspend fun run() {
        val apkScannerConfig = ApkScannerConfig.getConfig()

        val database = DatabaseUtil.getDatabase()

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

        DatabaseUtil.dropTables(database, apkScannerConfig.databaseConfig)
        DatabaseUtil.setupDatabase(database, apkScannerConfig.databaseConfig)

        // Import legacy data
        LibraryDataUtil.importLibraryData(database, apkScannerConfig)

        // Import certificates data
        CertificateDataUtil.importCertificateData(database, apkScannerConfig)
    }
}
