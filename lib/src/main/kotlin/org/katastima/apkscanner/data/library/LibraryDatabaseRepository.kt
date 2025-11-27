/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.data.library

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.katastima.apkscanner.database.dao.LibraryEntry
import org.katastima.apkscanner.database.dao.LibraryInformationEntry
import org.katastima.apkscanner.models.library.LegacyLibraryDefinition
import org.katastima.apkscanner.models.library.LegacyLibraryInformation

class LibraryDatabaseRepository(
    private val database: Database,
    private val backgroundDispatcher: CoroutineDispatcher,
    private val debugDatabase: Boolean = false,
) : LibraryRepository {

    override suspend fun getAllInformationEntries(): List<LegacyLibraryInformation> = dbQuery {
        return@dbQuery LibraryInformationEntry
            .all()
            .sortedBy { it.libraryId }
            .map { it.toLegacyLibraryInformation() }
    }

    override suspend fun countInformationEntries(): Long = dbQuery {
        return@dbQuery LibraryInformationEntry
            .all()
            .count()
    }

    override suspend fun getAllDefinitionEntries(): List<LegacyLibraryDefinition> = dbQuery {
        return@dbQuery LibraryEntry
            .all()
            .sortedBy { it.path }
            .map { it.toLegacyLibraryDefinition() }
    }

    override suspend fun countDefinitionEntries(): Long = dbQuery {
        return@dbQuery LibraryEntry
            .all()
            .count()
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
