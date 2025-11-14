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
import java.io.File

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
