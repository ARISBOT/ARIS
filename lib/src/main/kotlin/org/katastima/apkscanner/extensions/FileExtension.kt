/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.extensions

import okio.BufferedSource
import okio.HashingSink.Companion.sha256
import okio.blackholeSink
import okio.buffer
import okio.source
import org.katastima.apkscanner.config.ApkScannerConfig
import java.io.File
import java.nio.file.Paths

fun File.toSha256(): String {
    return source().buffer().toSha256()
}

fun BufferedSource.toSha256(): String {
    sha256(blackholeSink()).use { hashingSink ->
        this.use { source ->
            source.readAll(hashingSink)
            return hashingSink.hash.hex()
        }
    }
}

fun File.getApkFilePathForReport(apkScannerConfig: ApkScannerConfig): String = when (apkScannerConfig.scanConfig.apkReportedPathType) {
    "absolute" -> this.absolutePath

    "filename" -> this.name

    "relative" -> {
        val currentWorkingDirectory = Paths.get("").toAbsolutePath().toFile()
        this.relativeTo(currentWorkingDirectory).path
    }

    else -> this.name
}
