/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.extensions

import java.nio.BufferUnderflowException
import java.nio.ByteBuffer

@Throws(IllegalArgumentException::class)
fun ByteBuffer.sliceFromTo(start: Int, end: Int): ByteBuffer {
    require(start >= 0) { "start ($start) is not valid" }
    require(end >= start) { "end ($end) < start ($start)" }

    // Ensure we do not try to position outside of our capacity
    val capacity: Int = capacity()
    require(end <= capacity) { "end ($end) > capacity ($capacity)" }

    val originalLimit: Int = limit()
    val originalPosition: Int = position()
    try {
        // Position at 0 before repositioning
        position(0)

        // Set new limit and reposition at the specified start location
        limit(end)
        position(start)

        // Create a slice and set the same byte order for the new slice
        val result: ByteBuffer = slice()
        result.order(order())
        return result
    } finally {
        // Reset to original values
        position(0)
        limit(originalLimit)
        position(originalPosition)
    }
}

@Throws(BufferUnderflowException::class, IllegalArgumentException::class)
fun ByteBuffer.sliceWithSize(size: Int): ByteBuffer {
    require(size >= 0) { "size ($size) is not valid" }

    val originalLimit: Int = limit()
    val originalPosition: Int = position()
    val newLimit = originalPosition + size

    if (newLimit !in originalPosition..originalLimit) {
        throw BufferUnderflowException()
    }
    limit(newLimit)

    try {
        val result: ByteBuffer = slice()
        result.order(order())
        position(newLimit)
        return result
    } finally {
        limit(originalLimit)
    }
}
