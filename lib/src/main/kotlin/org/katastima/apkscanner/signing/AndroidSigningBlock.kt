/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.signing

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer

class AndroidSigningBlock(private val apkFile: File) {

    private val signingBlockValueIdMap: MutableMap<Int, ByteBuffer> = mutableMapOf()

    @Throws(IOException::class, RuntimeException::class)
    private fun readAndroidSigningBlock() {
        if (signingBlockValueIdMap.isNotEmpty()) {
            return
        }

        RandomAccessFile(apkFile, "r").use { randomAccessFile ->
            randomAccessFile.channel.use { fileChannel ->
                val androidSigningBlockPair = AndroidSigningBlockUtil.findApkSigningBlock(fileChannel)
                val idValueMap = AndroidSigningBlockUtil.getIdValuePairs(androidSigningBlockPair.first).toMap()
                signingBlockValueIdMap.putAll(idValueMap)
            }
        }
    }

    fun getBlockById(blockId: Int): Result<ByteBuffer?> {
        try {
            readAndroidSigningBlock()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return Result.success(signingBlockValueIdMap[blockId])
    }

    fun hasBlock(blockId: Int): Boolean = getBlockById(blockId).isSuccess

    fun getBlockSet(): Result<Set<Int>> {
        try {
            readAndroidSigningBlock()
        } catch (e: Exception) {
            return Result.failure(e)
        }

        val blockSet: MutableSet<Int> = mutableSetOf()

        signingBlockValueIdMap.forEach { entry ->
            blockSet.add(entry.key)
        }

        return Result.success(blockSet)
    }

    fun getBadBlockSet(): Result<Set<Int>> {
        try {
            readAndroidSigningBlock()
        } catch (e: Exception) {
            return Result.failure(e)
        }

        val blockSet: MutableSet<Int> = mutableSetOf()

        val badBlocks = getGoogleBlocks() + getPayloadBlocks()
        signingBlockValueIdMap.forEach { entry ->
            if (badBlocks.contains(entry.key)) {
                blockSet.add(entry.key)
            }
        }

        return Result.success(blockSet)
    }

    fun getUnknownBlockSet(): Result<Set<Int>> {
        try {
            readAndroidSigningBlock()
        } catch (e: Exception) {
            return Result.failure(e)
        }

        val unknownBlockSet: MutableSet<Int> = mutableSetOf()

        val allBlocks = getAllBlocks()
        signingBlockValueIdMap.forEach { entry ->
            if (!allBlocks.contains(entry.key)) {
                unknownBlockSet.add(entry.key)
            }
        }

        return Result.success(unknownBlockSet)
    }

    companion object {
        fun getAllBlocks(): Map<Int, String> = getOkBlocks() + getGoogleBlocks() + getPayloadBlocks()

        fun getOkBlocks(): Map<Int, String> = AndroidSigningBlockIds.OK_BLOCKS

        fun getGoogleBlocks(): Map<Int, String> = AndroidSigningBlockIds.GOOGLE_BLOCKS

        fun getPayloadBlocks(): Map<Int, String> = AndroidSigningBlockIds.PAYLOAD_BLOCKS
    }
}
