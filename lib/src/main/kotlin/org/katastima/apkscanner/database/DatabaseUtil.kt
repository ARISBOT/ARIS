/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.database

import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.katastima.apkscanner.config.DatabaseConfig
import org.katastima.apkscanner.config.DatabaseMode
import org.katastima.apkscanner.config.DatabaseType
import org.katastima.apkscanner.database.dao.LibraryInformationTable
import org.katastima.apkscanner.database.dao.LibraryTable
import org.katastima.apkscanner.database.dao.ManifestFilterConfigTable
import org.katastima.apkscanner.database.dao.ManifestFlagConfigTable
import org.katastima.apkscanner.database.dao.ManifestPermissionConfigTable
import org.katastima.apkscanner.database.dao.SigningCertificateDenylistTable
import org.slf4j.LoggerFactory
import java.io.File

object DatabaseUtil {

    private const val DRIVER_H2 = "org.h2.Driver"
    private const val URL_H2_MEMORY = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"

    private val LOGGER = LoggerFactory.getLogger(DatabaseUtil::class.java)

    private lateinit var _database: Database

    fun getDatabase(databaseConfig: DatabaseConfig): Database {
        if (!::_database.isInitialized) {
            _database = connectToDatabase(databaseConfig)
        }
        return _database
    }

    private fun connectToDatabase(databaseConfig: DatabaseConfig): Database {
        return when (databaseConfig.type) {
            DatabaseType.NONE -> {
                val exception = RuntimeException("There should not be any database access happening!")
                LOGGER.error("Using H2 memory database to ensure application keeps working for now", exception)

                Database.connect(URL_H2_MEMORY, driver = DRIVER_H2)
            }

            DatabaseType.H2 -> connectToH2Database(databaseConfig)
        }
    }

    private fun connectToH2Database(databaseConfig: DatabaseConfig): Database {
        return when (databaseConfig.mode) {
            DatabaseMode.DEFAULT -> {
                val databasePath = File(databaseConfig.path).absolutePath
                Database.connect("jdbc:h2:${databasePath}", driver = DRIVER_H2)
            }

            DatabaseMode.MEMORY -> Database.connect(URL_H2_MEMORY, driver = DRIVER_H2)
        }
    }

    fun setupDatabase(database: Database, debugDatabase: Boolean = false) {
        transaction(database) {
            if (debugDatabase) {
                addLogger(StdOutSqlLogger)
            }

            SchemaUtils.create(LibraryInformationTable)
            SchemaUtils.create(LibraryTable)

            SchemaUtils.create(ManifestFlagConfigTable)
            SchemaUtils.create(ManifestFilterConfigTable)
            SchemaUtils.create(ManifestPermissionConfigTable)

            SchemaUtils.create(SigningCertificateDenylistTable)
        }
    }

    fun dropTables(database: Database, debugDatabase: Boolean = false) {
        transaction(database) {
            if (debugDatabase) {
                addLogger(StdOutSqlLogger)
            }

            // Need to drop the library table first, as it references the information table
            SchemaUtils.drop(LibraryTable)
            SchemaUtils.drop(LibraryInformationTable)

            SchemaUtils.drop(ManifestFlagConfigTable)
            SchemaUtils.drop(ManifestFilterConfigTable)
            SchemaUtils.drop(ManifestPermissionConfigTable)

            SchemaUtils.drop(SigningCertificateDenylistTable)
        }
    }
}
