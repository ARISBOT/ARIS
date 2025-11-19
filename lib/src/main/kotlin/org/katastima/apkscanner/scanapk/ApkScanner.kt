/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.scanapk

import kotlinx.coroutines.CoroutineDispatcher
import org.jetbrains.exposed.v1.jdbc.Database
import org.katastima.apkscanner.config.ApkScannerConfig
import org.katastima.apkscanner.models.ApkScanResult
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.math.max
import kotlin.system.measureTimeMillis
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

        var totalScanDuration = 0L
        var totalProcessedApks = 0L

        apkFiles.forEach { apkFile ->
            val scanDuration = measureTimeMillis {
                val scanResult = scanSingle(apkFile)
                apkScanResultMap[apkFile] = scanResult

                // If callback is specified, invoke it.
                scanCallback?.invoke(apkFile, scanResult)
                totalProcessedApks++
            }
            totalScanDuration += scanDuration
            LOGGER.debug("scanDuration: {} ms", scanDuration)
        }
        LOGGER.debug(
            "Total scan duration: {} ms for {} apks ({} ms / apk)",
            totalScanDuration, totalProcessedApks, totalScanDuration / max(1, totalProcessedApks)
        )

        return apkScanResultMap
    }

    override fun close() {
        apkProcessor.close()
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ApkScanner::class.java)
    }
}
