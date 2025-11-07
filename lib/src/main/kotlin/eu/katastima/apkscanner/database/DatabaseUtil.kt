/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.database

import eu.katastima.apkscanner.config.ApkScannerConfig
import eu.katastima.apkscanner.config.DatabaseConfig
import eu.katastima.apkscanner.config.DatabaseMode
import eu.katastima.apkscanner.config.DatabaseType
import eu.katastima.apkscanner.database.dao.LibraryTable
import eu.katastima.apkscanner.database.dao.LibraryInformationTable
import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.File

object DatabaseUtil {

    private val _database: Database by lazy { connectToDatabase() }

    fun getDatabase(): Database = _database

    private fun connectToDatabase(): Database {
        val databaseConfig = ApkScannerConfig.getConfig().databaseConfig
        return when (databaseConfig.type) {
            DatabaseType.H2 -> connectToH2Database(databaseConfig)
            DatabaseType.SQLITE -> connectToSqliteDatabase(databaseConfig)
        }
    }

    private fun connectToH2Database(databaseConfig: DatabaseConfig): Database {
        return when (databaseConfig.mode) {
            DatabaseMode.DEFAULT -> {
                val databasePath = File(databaseConfig.path).absolutePath
                Database.connect("jdbc:h2:${databasePath}", driver = "org.h2.Driver")
            }

            DatabaseMode.MEMORY -> Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver")
        }
    }

    private fun connectToSqliteDatabase(databaseConfig: DatabaseConfig): Database {
        return when (databaseConfig.mode) {
            DatabaseMode.DEFAULT -> {
                val databasePath = File(databaseConfig.path).absolutePath
                Database.connect("jdbc:sqlite:${databasePath}", "org.sqlite.JDBC")
            }

            DatabaseMode.MEMORY -> Database.connect("jdbc:sqlite:file:test?mode=memory&cache=shared", "org.sqlite.JDBC")
        }
    }

    fun setupDatabase(database: Database, databaseConfig: DatabaseConfig) {
        transaction(database) {
            if (databaseConfig.debug) {
                addLogger(StdOutSqlLogger)
            }

            SchemaUtils.create(LibraryInformationTable)
            SchemaUtils.create(LibraryTable)
        }
    }
}
