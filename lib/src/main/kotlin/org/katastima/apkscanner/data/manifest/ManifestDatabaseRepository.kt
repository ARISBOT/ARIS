/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.data.manifest

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.katastima.apkscanner.database.dao.manifest.ManifestFilterConfigEntity
import org.katastima.apkscanner.database.dao.manifest.ManifestFilterConfigTable
import org.katastima.apkscanner.database.dao.manifest.ManifestFlagConfigEntity
import org.katastima.apkscanner.database.dao.manifest.ManifestFlagConfigTable
import org.katastima.apkscanner.database.dao.manifest.ManifestPermissionConfigEntity
import org.katastima.apkscanner.database.dao.manifest.ManifestPermissionConfigTable
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

    /** General */

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

    override suspend fun importManifestConfig(manifestConfig: ManifestConfig): Result<Long> = dbQuery {
        var counter = 0L

        // Flags
        manifestConfig.dangerousFlags.entries.sortedBy { it.name }.forEach {
            if (ManifestFlagConfigEntity.find { ManifestFlagConfigTable.name eq it.name }.empty()) {
                ManifestFlagConfigEntity.new {
                    name = it.name
                    description = it.description
                    flags = it.flags.sorted()
                }
                counter++
            }
        }

        // Filters
        manifestConfig.dangerousFilters.entries.sortedBy { it.name }.forEach {
            if (ManifestFilterConfigEntity.find { ManifestFilterConfigTable.name eq it.name }.empty()) {
                ManifestFilterConfigEntity.new {
                    name = it.name
                    description = it.description
                    filters = it.filters.sorted()
                }
                counter++
            }
        }

        // Permissions
        manifestConfig.dangerousPermissions.entries.sortedBy { it.name }.forEach {
            if (ManifestPermissionConfigEntity.find { ManifestPermissionConfigTable.name eq it.name }.empty()) {
                ManifestPermissionConfigEntity.new {
                    name = it.name
                    description = it.description
                    permissions = it.permissions.sorted()
                }
                counter++
            }
        }

        return@dbQuery Result.success(counter)
    }

    /** Flags */

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

    override suspend fun addFlagGroup(manifestFlagGroup: ManifestFlagConfigEntry): Result<Long> = dbQuery {
        val hasEntityWithName = ManifestFlagConfigEntity
            .find { ManifestFlagConfigTable.name eq manifestFlagGroup.name }
            .empty().not()
        if (hasEntityWithName) {
            return@dbQuery Result.failure(kotlin.RuntimeException("Manifest flag group with name (${manifestFlagGroup.name}) already exists!"))
        }

        try {
            val newEntity = ManifestFlagConfigEntity.new {
                name = manifestFlagGroup.name
                description = manifestFlagGroup.description
                flags = manifestFlagGroup.flags.toList()
            }
            return@dbQuery Result.success(newEntity.id.value.toLong())
        } catch (exc: Exception) {
            return@dbQuery Result.failure(exc)
        }
    }

    override suspend fun updateFlagGroup(manifestFlagGroup: ManifestFlagConfigEntry): Result<Long> = dbQuery {
        try {
            val updatedEntity = ManifestFlagConfigEntity
                .findSingleByAndUpdate(ManifestFlagConfigTable.name eq manifestFlagGroup.name) {
                    it.description = manifestFlagGroup.description
                    it.flags = manifestFlagGroup.flags.toList()
                }
            if (updatedEntity == null) {
                return@dbQuery Result.failure(kotlin.RuntimeException("Manifest flag group with name (${manifestFlagGroup.name}) does not exist!"))
            }
            return@dbQuery Result.success(updatedEntity.id.value.toLong())
        } catch (exc: Exception) {
            return@dbQuery Result.failure(exc)
        }
    }

    override suspend fun deleteFlagGroup(manifestFlagGroup: ManifestFlagConfigEntry): Result<Long> = dbQuery {
        try {
            val deletedEntity = ManifestFlagConfigEntity
                .findSingleByAndUpdate(ManifestFlagConfigTable.name eq manifestFlagGroup.name) {
                    it.delete()
                }
            if (deletedEntity == null) {
                return@dbQuery Result.failure(kotlin.RuntimeException("Manifest flag group with name (${manifestFlagGroup.name}) does not exist!"))
            }
            return@dbQuery Result.success(deletedEntity.id.value.toLong())
        } catch (exc: Exception) {
            return@dbQuery Result.failure(exc)
        }
    }

    /** IntentFilters */

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

    override suspend fun addIntentFilterGroup(intentFilterGroup: ManifestFilterConfigEntry): Result<Long> = dbQuery {
        val hasEntityWithName = ManifestFilterConfigEntity
            .find { ManifestFilterConfigTable.name eq intentFilterGroup.name }
            .empty().not()
        if (hasEntityWithName) {
            return@dbQuery Result.failure(kotlin.RuntimeException("Manifest intent filter group with name (${intentFilterGroup.name}) already exists!"))
        }

        try {
            val newEntity = ManifestFilterConfigEntity.new {
                name = intentFilterGroup.name
                description = intentFilterGroup.description
                filters = intentFilterGroup.filters.toList()
            }
            return@dbQuery Result.success(newEntity.id.value.toLong())
        } catch (exc: Exception) {
            return@dbQuery Result.failure(exc)
        }
    }

    override suspend fun updateIntentFilterGroup(intentFilterGroup: ManifestFilterConfigEntry): Result<Long> = dbQuery {
        try {
            val updatedEntity = ManifestFilterConfigEntity
                .findSingleByAndUpdate(ManifestFilterConfigTable.name eq intentFilterGroup.name) {
                    it.description = intentFilterGroup.description
                    it.filters = intentFilterGroup.filters.toList()
                }
            if (updatedEntity == null) {
                return@dbQuery Result.failure(kotlin.RuntimeException("Manifest intent filter group with name (${intentFilterGroup.name}) does not exist!"))
            }
            return@dbQuery Result.success(updatedEntity.id.value.toLong())
        } catch (exc: Exception) {
            return@dbQuery Result.failure(exc)
        }
    }

    override suspend fun deleteIntentFilterGroup(intentFilterGroup: ManifestFilterConfigEntry): Result<Long> = dbQuery {
        try {
            val deletedEntity = ManifestFilterConfigEntity
                .findSingleByAndUpdate(ManifestFilterConfigTable.name eq intentFilterGroup.name) {
                    it.delete()
                }
            if (deletedEntity == null) {
                return@dbQuery Result.failure(kotlin.RuntimeException("Manifest intent filter group with name (${intentFilterGroup.name}) does not exist!"))
            }
            return@dbQuery Result.success(deletedEntity.id.value.toLong())
        } catch (exc: Exception) {
            return@dbQuery Result.failure(exc)
        }
    }

    /** Permissions */

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

    override suspend fun addPermissionGroup(permissionGroup: ManifestPermissionConfigEntry): Result<Long> = dbQuery {
        val hasEntityWithName = ManifestPermissionConfigEntity
            .find { ManifestPermissionConfigTable.name eq permissionGroup.name }
            .empty().not()
        if (hasEntityWithName) {
            return@dbQuery Result.failure(kotlin.RuntimeException("Manifest permission group with name (${permissionGroup.name}) already exists!"))
        }

        try {
            val newEntity = ManifestPermissionConfigEntity.new {
                name = permissionGroup.name
                description = permissionGroup.description
                permissions = permissionGroup.permissions.toList()
            }
            return@dbQuery Result.success(newEntity.id.value.toLong())
        } catch (exc: Exception) {
            return@dbQuery Result.failure(exc)
        }
    }

    override suspend fun updatePermissionGroup(permissionGroup: ManifestPermissionConfigEntry): Result<Long> = dbQuery {
        try {
            val updatedEntity = ManifestPermissionConfigEntity
                .findSingleByAndUpdate(ManifestPermissionConfigTable.name eq permissionGroup.name) {
                    it.description = permissionGroup.description
                    it.permissions = permissionGroup.permissions.toList()
                }
            if (updatedEntity == null) {
                return@dbQuery Result.failure(kotlin.RuntimeException("Manifest permission group with name (${permissionGroup.name}) does not exist!"))
            }
            return@dbQuery Result.success(updatedEntity.id.value.toLong())
        } catch (exc: Exception) {
            return@dbQuery Result.failure(exc)
        }
    }

    override suspend fun deletePermissionGroup(permissionGroup: ManifestPermissionConfigEntry): Result<Long> = dbQuery {
        try {
            val deletedEntity = ManifestPermissionConfigEntity
                .findSingleByAndUpdate(ManifestPermissionConfigTable.name eq permissionGroup.name) {
                    it.delete()
                }
            if (deletedEntity == null) {
                return@dbQuery Result.failure(kotlin.RuntimeException("Manifest permission group with name (${permissionGroup.name}) does not exist!"))
            }
            return@dbQuery Result.success(deletedEntity.id.value.toLong())
        } catch (exc: Exception) {
            return@dbQuery Result.failure(exc)
        }
    }

    /** Helpers */

    private suspend fun <T> dbQuery(block: suspend () -> T): T = withContext(backgroundDispatcher) {
        return@withContext suspendTransaction(database) {
            if (debugDatabase) {
                addLogger(StdOutSqlLogger)
            }
            return@suspendTransaction block()
        }
    }
}
