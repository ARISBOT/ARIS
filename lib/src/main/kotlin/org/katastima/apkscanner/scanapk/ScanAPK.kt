/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.scanapk

import org.jetbrains.exposed.v1.jdbc.Database
import org.katastima.apkscanner.config.ApkScannerConfig
import java.io.Closeable
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.time.measureTimedValue

class ScanAPK(
    database: Database,
    workingDirectory: File = createTempDirectory().toFile(),
) : Closeable {

    private val apkScannerConfig: ApkScannerConfig by lazy { ApkScannerConfig.getConfig() }
    private val apkProcessor: ApkProcessor by lazy {
        ApkProcessor(
            apkScannerConfig = apkScannerConfig,
            database = database,
            workingDirectory = workingDirectory,
        )
    }

    fun scanSingle(apkFile: File): ApkScanResult {
        val (apkScanResult, timeTaken) = measureTimedValue {
            apkProcessor.processApk(apkFile)
        }

        return apkScanResult.copy(
            scanDurationMs = timeTaken.inWholeMilliseconds,
        )
    }

    fun scanMulti(apkFiles: List<File>, scanCallback: ((apkFile: File, scanResult: ApkScanResult) -> Unit)? = null): Map<File, ApkScanResult> {
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
