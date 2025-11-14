/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.signing

import org.katastima.apkscanner.extensions.sliceFromTo
import org.katastima.apkscanner.extensions.sliceWithSize
import org.katastima.apkscanner.utils.ZipUtil
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/** Adapted from https://android.googlesource.com/platform/frameworks/base/+/main/core/java/android/util/apk/ApkSigningBlockUtils.java */

object AndroidSigningBlockUtil {

    private val LOGGER = LoggerFactory.getLogger(AndroidSigningBlockUtil::class.java)

    /**
     * See: https://source.android.com/docs/security/features/apksigning/v2#apk-signing-block-format
     * Offset     Size        Description
     * 0          8           size of block in bytes (excluding this field)
     * 8          variable    sequence of uint64-length-prefixed ID-value pairs
     * -24        8           size of block in bytes - same as the very first field (uint64)
     * -16        16          Magic (APK Sig Block 42) - 41 50 4B 20 53 69 67 20 42 6C 6F 63 6B 20 34 32
     */

    const val APK_SIG_BLOCK_MIN_SIZE: Int = 8 + 0 + 8 + 16

    const val MAGIC_APK_SIG_BLOCK_LE_HIGH: Long = 0x3234206B636F6C42L
    const val MAGIC_APK_SIG_BLOCK_LE_LOW: Long = 0x20676953204b5041L

    @Throws(IllegalArgumentException::class, RuntimeException::class)
    fun getIdValuePairs(apkSigningBlockBuffer: ByteBuffer): MutableMap<Int, ByteBuffer> {
        require(apkSigningBlockBuffer.order() == ByteOrder.LITTLE_ENDIAN) { "ensure apk signing block buffer is little endian" }

        // Store the entries in order.
        val idValues: MutableMap<Int, ByteBuffer> = LinkedHashMap()

        // Start with an offset of 8 to get the variable sized field of pairs.
        val pairs: ByteBuffer = apkSigningBlockBuffer.sliceFromTo(8, apkSigningBlockBuffer.capacity() - 24)

        var entryCount = 0
        while (pairs.hasRemaining()) {
            entryCount++
            if (pairs.remaining() < 8) {
                throw RuntimeException("Not enough data to read block entry #$entryCount")
            }

            /**
             * The pair length is variable but at least 4 bytes long.
             * - ID (uint32)
             * - value (variable-length: length of the pair - 4 bytes)
             */
            val pairLength: Long = pairs.getLong()
            if (pairLength < 4 || pairLength > Int.MAX_VALUE) {
                throw RuntimeException("Block entry #$entryCount out of range - size: $pairLength")
            }

            val length = pairLength.toInt()
            val nextEntryPosition: Int = pairs.position() + length
            val available = pairs.remaining()
            if (length > available) {
                throw RuntimeException("Block entry #$entryCount out of range - length: $length, available: $available")
            }

            val id: Int = pairs.getInt()
            idValues[id] = pairs.sliceWithSize(length - 4)

            pairs.position(nextEntryPosition)
        }

        return idValues
    }

    @Throws(IOException::class, RuntimeException::class)
    fun findApkSigningBlock(fileChannel: FileChannel, centralDirStartOffset: Long = ZipUtil.findZipCentralDirStartOffset(fileChannel)): Pair<ByteBuffer, Long> {
        if (centralDirStartOffset < APK_SIG_BLOCK_MIN_SIZE) {
            throw RuntimeException("File too small to be able to fit the apk signature block, offset: $centralDirStartOffset")
        }

        // Check the footer of the block (-16 + -8) for the magic
        fileChannel.position(centralDirStartOffset - 24)
        val footerBuffer = ByteBuffer.allocate(24)
        fileChannel.read(footerBuffer)
        footerBuffer.order(ByteOrder.LITTLE_ENDIAN)

        // Check for the magic
        if (footerBuffer.getLong(8) != MAGIC_APK_SIG_BLOCK_LE_LOW || footerBuffer.getLong(16) != MAGIC_APK_SIG_BLOCK_LE_HIGH) {
            throw RuntimeException("Could not find APK sig block magic")
        }

        // Read and compare size fields
        val apkSigBlockSizeFooter = footerBuffer.getLong(0)
        if (apkSigBlockSizeFooter < footerBuffer.capacity() || apkSigBlockSizeFooter > Int.MAX_VALUE - 8) {
            throw RuntimeException("Block out of range: $apkSigBlockSizeFooter")
        }

        // Get the total size of the signature block which will be used for offset calculation and the block allocation.
        val totalSize = (apkSigBlockSizeFooter + 8).toInt()

        // Calculate the offset of the signature block and position the file channel.
        val apkSigBlockOffset = centralDirStartOffset - totalSize
        if (apkSigBlockOffset < 0) {
            throw RuntimeException("Block offset out of range: $apkSigBlockOffset")
        }
        fileChannel.position(apkSigBlockOffset)

        // Get the block!
        val apkSigBlock = ByteBuffer.allocate(totalSize)
        fileChannel.read(apkSigBlock)
        apkSigBlock.order(ByteOrder.LITTLE_ENDIAN)

        // Final verification to check if header and footer sizes are matching.
        val apkSigBlockSizeHeader = apkSigBlock.getLong(0)
        if (apkSigBlockSizeHeader != apkSigBlockSizeFooter) {
            throw RuntimeException("Block sizes of header ($apkSigBlockSizeHeader) and footer ($apkSigBlockSizeFooter) do not match!")
        }

        return Pair(apkSigBlock, apkSigBlockOffset)
    }

    fun getBytes(byteBuffer: ByteBuffer): ByteArray {
        val arrayOffset = byteBuffer.arrayOffset()
        return byteBuffer.array().copyOfRange(arrayOffset + byteBuffer.position(), arrayOffset + byteBuffer.limit())
    }

    fun getString(byteBuffer: ByteBuffer): String = try {
        val bytes = getBytes(byteBuffer)
        String(bytes, Charsets.UTF_8)
    } catch (exc: Exception) {
        LOGGER.error("Could not get string from bytes", exc)
        ""
    }
}
