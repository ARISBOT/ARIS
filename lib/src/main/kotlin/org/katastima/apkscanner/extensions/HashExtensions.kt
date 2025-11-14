/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.extensions

import java.security.MessageDigest

private fun ByteArray.hash(algorithm: String) = MessageDigest.getInstance(algorithm)
    .digest(this)
    .toHexString()

private fun ByteArray.hashHexed(algorithm: String) = MessageDigest.getInstance(algorithm)
    .digest(this.toHexString().toByteArray())
    .toHexString()

fun ByteArray.toMd5() = hash("MD5")
fun ByteArray.toHexMd5() = hashHexed("MD5")
fun ByteArray.toSha1() = hash("SHA-1")
fun ByteArray.toHexSha1() = hashHexed("SHA-1")
fun ByteArray.toSha256() = hash("SHA-256")
fun ByteArray.toHexSha256() = hashHexed("SHA-256")

fun String.toMd5() = toByteArray().toMd5()
fun String.toSha1() = toByteArray().toSha1()
fun String.toSha256() = toByteArray().toSha256()
