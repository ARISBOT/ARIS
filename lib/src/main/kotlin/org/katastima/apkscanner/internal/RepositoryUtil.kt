/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.internal

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.jdbc.Database
import org.katastima.apkscanner.config.ApkScannerConfig
import org.katastima.apkscanner.data.library.LibraryDatabaseRepository
import org.katastima.apkscanner.data.library.LibraryRepository
import org.katastima.apkscanner.data.manifest.ManifestDatabaseRepository
import org.katastima.apkscanner.data.manifest.ManifestRepository

object RepositoryUtil {

    fun getManifestRepository(
        database: Database,
        apkScannerConfig: ApkScannerConfig,
        backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
    ): ManifestRepository {
        // TODO: different repo impl based on config.
        return ManifestDatabaseRepository(database, backgroundDispatcher, apkScannerConfig.databaseConfig.debug)
    }

    fun getLibraryRepository(
        database: Database,
        apkScannerConfig: ApkScannerConfig,
        backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
    ): LibraryRepository {
        // TODO: different repo impl based on config.
        return LibraryDatabaseRepository(database, backgroundDispatcher, apkScannerConfig.databaseConfig.debug)
    }
}
