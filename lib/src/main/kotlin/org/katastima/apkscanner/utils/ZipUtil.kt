/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.utils

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.min

object ZipUtil {

    /**
     * End of central directory record (EOCD)
     * See: https://en.wikipedia.org/wiki/ZIP_(file_format)#End_of_central_directory_record_(EOCD)
     *
     * Offset    Size    Description
     * 0         4       Magic number. Must be 50 4B 05 06.
     * 4         2       Number of this disk
     * 6         2       Disk where central directory starts
     * 8         2       Number of central directory records on this disk
     * 10        2       Total number of central directory records
     * 12        4       Size of central directory (bytes)
     * 16        4       Offset of start of central directory, relative to start of archive
     * 20        2       Comment length (n)
     * 22        n       Comment
     *
     * Without any archive comments, the EOCD should be 22 bytes long.
     */

    // Magic number but in reverse order, because LE/BE is a thing :/
    const val ZIP_EOCD_RECORD_SIGNATURE = 0x06054B50

    // According to the EOCD documentation, the record needs to be at least 22 bytes in size.
    const val ZIP_EOCD_RECORD_MINIMUM_SIZE = 22

    // According to the EOCD documentation, the comment length field has an offset of 20 bytes.
    const val ZIP_EOCD_COMMENT_LENGTH_OFFSET = 20

    // The comment field is an uint16, so it has a max size of 65535 bytes.
    const val ZIP_EOCD_COMMENT_LENGTH_MAX_VALUE: Long = 0xffff

    fun getZipCommentLength(fileChannel: FileChannel): Long {
        // Ensure the file is big enough to even fit the EOCD record.
        val archiveSize: Long = fileChannel.size()
        if (archiveSize < ZIP_EOCD_RECORD_MINIMUM_SIZE) {
            throw IOException("Archive is smaller than the minimum required size for the EOCD record.")
        }

        // As the comment field does not have a fixed size, we need to search for it in backwards order.
        val maxCommentLength: Long = min(archiveSize - ZIP_EOCD_RECORD_MINIMUM_SIZE, ZIP_EOCD_COMMENT_LENGTH_MAX_VALUE)
        val archiveStartPosition: Long = archiveSize - ZIP_EOCD_RECORD_MINIMUM_SIZE
        for (expectedCommentLength in 0..maxCommentLength) {
            // Get closer to figuring out the start position, step by step.
            val eocdStartPosition = archiveStartPosition - expectedCommentLength

            val byteBuffer: ByteBuffer = ByteBuffer.allocate(4)
            fileChannel.position(eocdStartPosition)
            fileChannel.read(byteBuffer)
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN)

            // We found the EOCD record signature.
            if (byteBuffer.getInt(0) == ZIP_EOCD_RECORD_SIGNATURE) {
                // The comment length is 2 bytes.
                val commentLengthBuffer: ByteBuffer = ByteBuffer.allocate(2)
                fileChannel.position(eocdStartPosition + ZIP_EOCD_COMMENT_LENGTH_OFFSET)
                fileChannel.read(commentLengthBuffer)
                commentLengthBuffer.order(ByteOrder.LITTLE_ENDIAN)

                val actualCommentLength: Long = commentLengthBuffer.getShort(0).toLong()
                if (actualCommentLength == expectedCommentLength) {
                    return actualCommentLength
                }
            }
        }
        throw IOException("Could not find EOCD record")
    }

    @Throws(IOException::class)
    fun findZipCentralDirStartOffset(fileChannel: FileChannel, commentLength: Long = getZipCommentLength(fileChannel)): Long {
        val centralDirectoryBuffer = ByteBuffer.allocate(4)
        centralDirectoryBuffer.order(ByteOrder.LITTLE_ENDIAN)

        val startOfCentralDirectory = 2 /* Comment length */ + 4 /* Offset of start of central directory, relative to start of archive */
        fileChannel.position(fileChannel.size() - commentLength - startOfCentralDirectory)
        fileChannel.read(centralDirectoryBuffer)

        return centralDirectoryBuffer.getInt(0).toLong()
    }
}
