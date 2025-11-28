/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.data.library

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.katastima.apkscanner.database.dao.library.LibraryEntry
import org.katastima.apkscanner.database.dao.library.LibraryInformationEntry
import org.katastima.apkscanner.database.dao.library.LibraryInformationTable
import org.katastima.apkscanner.database.dao.library.LibraryTable
import org.katastima.apkscanner.models.library.LegacyLibraryDefinition
import org.katastima.apkscanner.models.library.LegacyLibraryInformation
import org.katastima.apkscanner.models.library.LibraryInformation

class LibraryDatabaseRepository(
    private val database: Database,
    private val backgroundDispatcher: CoroutineDispatcher,
    private val debugDatabase: Boolean = false,
) : LibraryRepository {

    /** Information Entries */

    override suspend fun importInformationEntries(informationEntries: List<LegacyLibraryInformation>): Result<Long> = dbQuery {
        var counter = 0L

        informationEntries
            .sortedBy { it.id.lowercase() }
            .forEach {
                if (LibraryInformationEntry.find { LibraryInformationTable.libraryId eq it.id }.empty()) {
                    LibraryInformationEntry.new {
                        libraryId = it.id
                        name = ""
                        details = it.details
                        type = ""
                        permissions = emptyList()
                        url = ""
                        modWarningId = it.modWarningId
                        antiFeatures = it.antiFeatures.asList()
                        license = it.license
                        emphasize = it.emphasize
                    }
                    counter++
                }
            }

        return@dbQuery Result.success(counter)
    }

    override suspend fun getAllInformationEntries(offset: Int, count: Int): List<LegacyLibraryInformation> = dbQuery {
        var informationEntries = LibraryInformationEntry
            .all()
            .orderBy(LibraryInformationTable.libraryId.lowerCase() to SortOrder.ASC)

        // If an offset is specified, use it.
        if (offset > 0) {
            informationEntries = informationEntries.offset(offset.toLong())
        }

        // If a limit is specified, limit.
        if (count > 0) {
            informationEntries = informationEntries.limit(count)
        }

        return@dbQuery informationEntries.map { it.toLegacyLibraryInformation() }
    }

    override suspend fun countInformationEntries(): Long = dbQuery {
        return@dbQuery LibraryInformationEntry
            .all()
            .count()
    }

    /** Definition Entries */

    override suspend fun importDefinitionEntries(definitionEntries: List<LegacyLibraryDefinition>): Result<Long> = dbQuery {
        var counter = 0L

        definitionEntries
            .sortedBy { it.id.lowercase() }
            .forEach {
                LibraryInformationEntry
                    .find { LibraryInformationTable.libraryId eq it.id }
                    .forEach { foundDefinition ->
                        val permissionSet = hashSetOf<String>()
                        permissionSet.addAll(foundDefinition.permissions)
                        permissionSet.addAll(it.perms)

                        foundDefinition.apply {
                            name = it.name
                            type = it.type
                            permissions = permissionSet.sorted()
                            url = it.url
                        }

                        if (LibraryEntry.find { LibraryTable.path eq it.path }.empty()) {
                            LibraryEntry.new {
                                path = it.path
                                libraryInformationEntry = foundDefinition
                            }
                        }

                        counter++
                    }
            }

        return@dbQuery Result.success(counter)
    }

    override suspend fun getAllDefinitionEntries(offset: Int, count: Int): List<LegacyLibraryDefinition> = dbQuery {
        var definitionEntries = LibraryEntry
            .all()
            .orderBy(LibraryTable.path.lowerCase() to SortOrder.ASC)

        // If an offset is specified, use it.
        if (offset > 0) {
            definitionEntries = definitionEntries.offset(offset.toLong())
        }

        // If a limit is specified, limit.
        if (count > 0) {
            definitionEntries = definitionEntries.limit(count)
        }

        return@dbQuery definitionEntries.map { it.toLegacyLibraryDefinition() }
    }

    override suspend fun countDefinitionEntries(): Long = dbQuery {
        return@dbQuery LibraryEntry
            .all()
            .count()
    }

    /** Helpers */

    override suspend fun getLibraryInformationForLibraryPath(libraryPath: String): List<LibraryInformation> = dbQuery {
        return@dbQuery LibraryEntry
            .find { LibraryTable.path eq libraryPath }
            .map { it.libraryInformationEntry.toLibraryInformation() }
            .sortedBy { it.name }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T = withContext(backgroundDispatcher) {
        return@withContext suspendTransaction(database) {
            if (debugDatabase) {
                addLogger(StdOutSqlLogger)
            }
            return@suspendTransaction block()
        }
    }
}
