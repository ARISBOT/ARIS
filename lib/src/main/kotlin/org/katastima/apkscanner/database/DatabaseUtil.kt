/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.database

import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.katastima.apkscanner.config.DatabaseConfig
import org.katastima.apkscanner.config.DatabaseMode
import org.katastima.apkscanner.config.DatabaseType
import org.katastima.apkscanner.database.dao.LibraryEntry
import org.katastima.apkscanner.database.dao.LibraryInformationTable
import org.katastima.apkscanner.database.dao.LibraryTable
import org.katastima.apkscanner.database.dao.ManifestFilterConfigTable
import org.katastima.apkscanner.database.dao.ManifestFlagConfigTable
import org.katastima.apkscanner.database.dao.ManifestPermissionConfigTable
import org.katastima.apkscanner.database.dao.SigningCertificateDenylistTable
import org.katastima.apkscanner.models.library.LibraryInformation
import org.slf4j.LoggerFactory
import java.io.File

object DatabaseUtil {

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

                Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver")
            }

            DatabaseType.H2 -> connectToH2Database(databaseConfig)
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

    fun getLibraryInformationFromLibraryPath(database: Database, libraryPath: String, debugDatabase: Boolean = false): Set<LibraryInformation> {
        val libraryInformationSet = mutableSetOf<LibraryInformation>()

        transaction(database) {
            if (debugDatabase) {
                addLogger(StdOutSqlLogger)
            }

            LibraryEntry
                .find { LibraryTable.path eq libraryPath }
                .forEach { libraryEntry ->
                    val libraryInformation = libraryEntry.libraryInformationEntry.toLibraryInformation()
                    libraryInformationSet.add(libraryInformation)
                }
        }

        return libraryInformationSet
    }
}
