/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.database

import eu.katastima.apkscanner.config.ApkScannerConfig
import eu.katastima.apkscanner.config.DatabaseConfig
import eu.katastima.apkscanner.config.DatabaseMode
import eu.katastima.apkscanner.config.DatabaseType
import eu.katastima.apkscanner.database.dao.*
import eu.katastima.apkscanner.models.LibraryInformation
import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.core.eq
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

            SchemaUtils.create(SigningCertificateAllowlistTable)
            SchemaUtils.create(SigningCertificateDenylistTable)
        }
    }

    fun dropTables(database: Database, databaseConfig: DatabaseConfig) {
        transaction(database) {
            if (databaseConfig.debug) {
                addLogger(StdOutSqlLogger)
            }

            SchemaUtils.drop(LibraryInformationTable)
            SchemaUtils.drop(LibraryTable)

            SchemaUtils.drop(SigningCertificateAllowlistTable)
            SchemaUtils.drop(SigningCertificateDenylistTable)
        }
    }

    fun getLibraryInformationFromLibraryPath(database: Database, databaseConfig: DatabaseConfig, libraryPath: String): Set<LibraryInformation> {
        val libraryInformationSet = mutableSetOf<LibraryInformation>()

        transaction(database) {
            if (databaseConfig.debug) {
                addLogger(StdOutSqlLogger)
            }

            val informationMap = mutableMapOf<String, LibraryInformationEntry>()
            LibraryEntry
                .find { LibraryTable.path eq libraryPath }
                .forEach { libraryEntry ->
                    if (informationMap[libraryEntry.libraryId] == null) {
                        LibraryInformationEntry
                            .find { LibraryInformationTable.libraryId eq libraryEntry.libraryId }
                            .forEach { libraryInformationEntry ->
                                informationMap[libraryEntry.libraryId] = libraryInformationEntry
                            }
                    }
                }

            informationMap.values.forEach { informationEntry ->
                val libraryInformation = LibraryInformation(
                    libraryId = informationEntry.libraryId,
                    name = informationEntry.name,
                    details = informationEntry.details,
                    type = informationEntry.type,
                    permissions = informationEntry.permissions.toTypedArray(),
                    url = informationEntry.url,
                    modWarningId = informationEntry.modWarningId,
                    antiFeatures = informationEntry.antiFeatures.toTypedArray(),
                    license = informationEntry.license,
                    emphasize = informationEntry.emphasize,
                )
                libraryInformationSet.add(libraryInformation)
            }
        }

        return libraryInformationSet
    }
}
