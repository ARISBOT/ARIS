/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.data.manifest

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.katastima.apkscanner.database.dao.ManifestFilterConfigEntity
import org.katastima.apkscanner.database.dao.ManifestFilterConfigTable
import org.katastima.apkscanner.database.dao.ManifestFlagConfigEntity
import org.katastima.apkscanner.database.dao.ManifestFlagConfigTable
import org.katastima.apkscanner.database.dao.ManifestPermissionConfigEntity
import org.katastima.apkscanner.database.dao.ManifestPermissionConfigTable
import org.katastima.apkscanner.models.manifest.config.ManifestConfig
import org.katastima.apkscanner.models.manifest.config.ManifestFilterConfig
import org.katastima.apkscanner.models.manifest.config.ManifestFilterConfigEntry
import org.katastima.apkscanner.models.manifest.config.ManifestFlagConfig
import org.katastima.apkscanner.models.manifest.config.ManifestFlagConfigEntry
import org.katastima.apkscanner.models.manifest.config.ManifestPermissionConfig
import org.katastima.apkscanner.models.manifest.config.ManifestPermissionConfigEntry

class ManifestDatabaseRepository(
    private val database: Database,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val debugDatabase: Boolean = false,
) : ManifestRepository {

    override suspend fun getManifestConfig(): ManifestConfig = dbQuery {
        return@dbQuery ManifestConfig(
            dangerousFlags = ManifestFlagConfig(
                entries = getAllFlagGroups().toSet(),
            ),
            dangerousFilters = ManifestFilterConfig(
                entries = getAllIntentFilterGroups().toSet(),
            ),
            dangerousPermissions = ManifestPermissionConfig(
                entries = getAllPermissionGroups().toSet(),
            ),
        )
    }

    override suspend fun getAllFlagGroups(): List<ManifestFlagConfigEntry> = dbQuery {
        return@dbQuery ManifestFlagConfigEntity
            .all()
            .sortedBy { ManifestFlagConfigTable.name }
            .map { it.toManifestFlagConfigEntry() }
    }

    override suspend fun countAllFlagGroups(): Long = dbQuery {
        return@dbQuery ManifestFlagConfigEntity
            .all()
            .count()
    }

    override suspend fun countAllFlags(): Long = dbQuery {
        return@dbQuery ManifestFlagConfigEntity
            .all()
            .fold(0) { count, entity -> count + entity.flags.count() }
    }

    override suspend fun getAllIntentFilterGroups(): List<ManifestFilterConfigEntry> = dbQuery {
        return@dbQuery ManifestFilterConfigEntity
            .all()
            .sortedBy { ManifestFilterConfigTable.name }
            .map { it.toManifestFilterConfigEntry() }
    }

    override suspend fun countAllIntentFilterGroups(): Long = dbQuery {
        return@dbQuery ManifestFilterConfigEntity
            .all()
            .count()
    }

    override suspend fun countAllIntentFilters(): Long = dbQuery {
        return@dbQuery ManifestFilterConfigEntity
            .all()
            .fold(0) { count, entity -> count + entity.filters.count() }
    }

    override suspend fun getAllPermissionGroups(): List<ManifestPermissionConfigEntry> = dbQuery {
        return@dbQuery ManifestPermissionConfigEntity
            .all()
            .sortedBy { ManifestPermissionConfigTable.name }
            .map { it.toManifestPermissionConfigEntry() }
    }

    override suspend fun countAllPermissionGroups(): Long = dbQuery {
        return@dbQuery ManifestPermissionConfigEntity
            .all()
            .count()
    }

    override suspend fun countAllPermissions(): Long = dbQuery {
        return@dbQuery ManifestPermissionConfigEntity
            .all()
            .fold(0) { count, entity -> count + entity.permissions.count() }
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
