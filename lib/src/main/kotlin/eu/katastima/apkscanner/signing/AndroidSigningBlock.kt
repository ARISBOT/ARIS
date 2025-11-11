/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.signing

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.util.logging.Level
import java.util.logging.Logger

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
                    val idValueMap = AndroidSigningBlockUtil.getIdValuePairs(androidSigningBlockPair.first)
                    signingBlockValueIdMap.putAll(idValueMap)
                } catch (exc: Exception) {
                    LOGGER.log(Level.SEVERE, "Could not read APK signing block", exc)
                }
            }
        }
    }

    fun getBlockById(blockId: Int): ByteBuffer? {
        readAndroidSigningBlock()
        return signingBlockValueIdMap[blockId]
    }

    fun hasBlock(blockId: Int): Boolean = getBlockById(blockId) != null

    fun getOkBlocks(): Map<Int, String> = AndroidSigningBlockIds.OK_BLOCKS

    fun getGoogleBlocks(): Map<Int, String> = AndroidSigningBlockIds.GOOGLE_BLOCKS

    fun getPayloadBlocks(): Map<Int, String> = AndroidSigningBlockIds.PAYLOAD_BLOCKS

    companion object {
        private val LOGGER = Logger.getLogger(AndroidSigningBlock::class.simpleName)
    }
}
