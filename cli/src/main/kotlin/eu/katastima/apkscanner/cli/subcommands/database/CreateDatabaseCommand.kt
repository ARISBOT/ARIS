/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.cli.subcommands.database

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import eu.katastima.apkscanner.config.ApkScannerConfig
import eu.katastima.apkscanner.database.DatabaseUtil
import eu.katastima.apkscanner.utils.LegacyUtil
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.File

class CreateDatabaseCommand : CliktCommand("setup") {

    val forceSetup by option("--force", "-f")
        .flag()
        .help("Force database setup without waiting when data already exists")

    override fun help(context: Context): String = "Setup the database for usage, removing existing data."

    override fun run() {
        val apkScannerConfig = ApkScannerConfig.getConfig()

        val database = DatabaseUtil.getDatabase()

        if (!forceSetup) {
            val tablesExist = transaction(database) {
                SchemaUtils.listTables().isNotEmpty()
            }
            if (tablesExist) {
                echo("WARNING: existing data found, which will be destroyed!")
                echo("Terminate the application, if this was a mistake!")
                echo("Waiting for 30 seconds (this can be skipped with using the force option).")
                // TODO: actually wait 30 seconds.
                echo()
            }
        }

        DatabaseUtil.dropTables(database, apkScannerConfig.databaseConfig)
        DatabaseUtil.setupDatabase(database, apkScannerConfig.databaseConfig)

        val legacyConfig = apkScannerConfig.legacyConfig
        if (File(legacyConfig.libraryInformationPath).exists() && File(legacyConfig.libraryDefinitionPath).exists()) {
            LegacyUtil.importLegacyData(database, apkScannerConfig)
        }
    }
}
