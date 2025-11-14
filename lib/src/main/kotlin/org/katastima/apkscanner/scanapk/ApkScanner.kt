/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.scanapk

import kotlinx.coroutines.CoroutineDispatcher
import org.jetbrains.exposed.v1.jdbc.Database
import org.katastima.apkscanner.config.ApkScannerConfig
import java.io.Closeable
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.time.measureTimedValue

class ApkScanner(
    apkScannerConfig: ApkScannerConfig,
    database: Database,
    private val backgroundDispatcher: CoroutineDispatcher,
    private val ioDispatcher: CoroutineDispatcher,
    workingDirectory: File = createTempDirectory().toFile(),
) : Closeable {

    private val apkProcessor: ApkProcessor by lazy {
        ApkProcessor(
            apkScannerConfig = apkScannerConfig,
            database = database,
            backgroundDispatcher = backgroundDispatcher,
            ioDispatcher = ioDispatcher,
            workingDirectory = workingDirectory,
        )
    }

    suspend fun scanSingle(apkFile: File): ApkScanResult {
        val (apkScanResult, timeTaken) = measureTimedValue {
            apkProcessor.processApk(apkFile)
        }

        return apkScanResult.copy(
            scanDurationMs = timeTaken.inWholeMilliseconds,
        )
    }

    suspend fun scanMulti(apkFiles: List<File>, scanCallback: ((apkFile: File, scanResult: ApkScanResult) -> Unit)? = null): Map<File, ApkScanResult> {
        val apkScanResultMap: MutableMap<File, ApkScanResult> = mutableMapOf()
        apkFiles.forEach { apkFile ->
            val scanResult = scanSingle(apkFile)
            apkScanResultMap[apkFile] = scanResult

            // If callback is specified, invoke it.
            scanCallback?.invoke(apkFile, scanResult)
        }
        return apkScanResultMap
    }

    override fun close() {
        apkProcessor.close()
    }
}
