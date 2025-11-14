/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.signing

import org.slf4j.LoggerFactory
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer

class AndroidSigningBlock(private val apkFile: File) {

    private val signingBlockValueIdMap: MutableMap<Int, ByteBuffer> = mutableMapOf()

    private fun readAndroidSigningBlock() {
        if (signingBlockValueIdMap.isNotEmpty()) {
            return
        }

        RandomAccessFile(apkFile, "r").use { randomAccessFile ->
            randomAccessFile.channel.use { fileChannel ->
                try {
                    val androidSigningBlockPair = AndroidSigningBlockUtil.findApkSigningBlock(fileChannel)
                    val idValueMap = AndroidSigningBlockUtil.getIdValuePairs(androidSigningBlockPair.first).toMap()
                    signingBlockValueIdMap.putAll(idValueMap)
                } catch (exc: Exception) {
                    LOGGER.error("Could not read APK signing block", exc)
                }
            }
        }
    }

    fun getBlockById(blockId: Int): ByteBuffer? {
        readAndroidSigningBlock()
        return signingBlockValueIdMap[blockId]
    }

    fun hasBlock(blockId: Int): Boolean = getBlockById(blockId) != null

    fun getBlockSet(): Set<Int> {
        readAndroidSigningBlock()

        val blockSet: MutableSet<Int> = mutableSetOf()

        signingBlockValueIdMap.forEach { entry ->
            blockSet.add(entry.key)
        }

        return blockSet
    }

    fun getUnknownBlockSet(): Set<Int> {
        readAndroidSigningBlock()

        val unknownBlockSet: MutableSet<Int> = mutableSetOf()

        val allBlocks = getAllBlocks()
        signingBlockValueIdMap.forEach { entry ->
            if (!allBlocks.contains(entry.key)) {
                unknownBlockSet.add(entry.key)
            }
        }

        return unknownBlockSet
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AndroidSigningBlock::class.java)

        fun getAllBlocks(): Map<Int, String> = getOkBlocks() + getGoogleBlocks() + getPayloadBlocks()

        fun getOkBlocks(): Map<Int, String> = AndroidSigningBlockIds.OK_BLOCKS

        fun getGoogleBlocks(): Map<Int, String> = AndroidSigningBlockIds.GOOGLE_BLOCKS

        fun getPayloadBlocks(): Map<Int, String> = AndroidSigningBlockIds.PAYLOAD_BLOCKS
    }
}
