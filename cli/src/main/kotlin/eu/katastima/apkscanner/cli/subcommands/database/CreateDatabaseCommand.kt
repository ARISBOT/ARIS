/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.cli.subcommands.database

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import eu.katastima.apkscanner.config.ApkScannerConfig
import eu.katastima.apkscanner.database.DatabaseUtil
import eu.katastima.apkscanner.utils.LegacyUtil
import java.io.File

class CreateDatabaseCommand : CliktCommand("setup") {

    override fun help(context: Context): String = "Setup the database."

    override fun run() {
        val apkScannerConfig = ApkScannerConfig.getConfig()

        val database = DatabaseUtil.getDatabase()
        DatabaseUtil.setupDatabase(database, apkScannerConfig.databaseConfig)

        val legacyConfig = apkScannerConfig.legacyConfig
        if (File(legacyConfig.libraryInformationPath).exists() && File(legacyConfig.libraryDefinitionPath).exists()) {
            LegacyUtil.importLegacyData(database, apkScannerConfig)
        }
    }
}
