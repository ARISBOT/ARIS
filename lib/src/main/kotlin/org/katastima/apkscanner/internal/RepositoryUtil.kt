/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.internal

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.katastima.apkscanner.config.ApkScannerConfig
import org.katastima.apkscanner.config.DatabaseType
import org.katastima.apkscanner.data.certificate.CertificateDatabaseRepository
import org.katastima.apkscanner.data.certificate.CertificateFileRepository
import org.katastima.apkscanner.data.certificate.CertificateRepository
import org.katastima.apkscanner.data.library.LibraryDatabaseRepository
import org.katastima.apkscanner.data.library.LibraryFileRepository
import org.katastima.apkscanner.data.library.LibraryRepository
import org.katastima.apkscanner.data.manifest.ManifestDatabaseRepository
import org.katastima.apkscanner.data.manifest.ManifestFileRepository
import org.katastima.apkscanner.data.manifest.ManifestRepository
import org.katastima.apkscanner.database.DatabaseUtil

object RepositoryUtil {

    fun getCertificateRepository(
        apkScannerConfig: ApkScannerConfig,
        backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
    ): CertificateRepository = when (apkScannerConfig.databaseConfig.type) {
        DatabaseType.NONE -> CertificateFileRepository(apkScannerConfig.dataConfig)
        else -> {
            val database = DatabaseUtil.getDatabase(apkScannerConfig.databaseConfig)
            DatabaseUtil.setupDatabase(database, apkScannerConfig, apkScannerConfig.databaseConfig.debug)

            CertificateDatabaseRepository(database, backgroundDispatcher, apkScannerConfig.databaseConfig.debug)
        }
    }

    fun getLibraryRepository(
        apkScannerConfig: ApkScannerConfig,
        backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
    ): LibraryRepository = when (apkScannerConfig.databaseConfig.type) {
        DatabaseType.NONE -> LibraryFileRepository(apkScannerConfig.dataConfig)
        else -> {
            val database = DatabaseUtil.getDatabase(apkScannerConfig.databaseConfig)
            DatabaseUtil.setupDatabase(database, apkScannerConfig, apkScannerConfig.databaseConfig.debug)

            LibraryDatabaseRepository(database, backgroundDispatcher, apkScannerConfig.databaseConfig.debug)
        }
    }

    fun getManifestRepository(
        apkScannerConfig: ApkScannerConfig,
        backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
    ): ManifestRepository = when (apkScannerConfig.databaseConfig.type) {
        DatabaseType.NONE -> ManifestFileRepository(apkScannerConfig.dataConfig)
        else -> {
            val database = DatabaseUtil.getDatabase(apkScannerConfig.databaseConfig)
            DatabaseUtil.setupDatabase(database, apkScannerConfig, apkScannerConfig.databaseConfig.debug)

            ManifestDatabaseRepository(database, backgroundDispatcher, apkScannerConfig.databaseConfig.debug)
        }
    }
}
