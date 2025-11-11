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
                    signingBlockValueIdMap.putAll(AndroidSigningBlockUtil.getIdValuePairs(androidSigningBlockPair.first))
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

    fun getGoogleBlocks(): Map<Int, String> = GOOGLE_BLOCKS

    fun getPayloadBlocks(): Map<Int, String> = PAYLOAD_BLOCKS

    companion object {
        private val LOGGER = Logger.getLogger(AndroidSigningBlock::class.simpleName)

        /**
         * https://developer.android.com/build/dependencies#dependency-info-play
         * https://developer.android.com/build/dependency-verification
         * https://android.googlesource.com/platform/tools/base/+/c71ae138365bcec912656fb39b9cf27fd8be567d/signflinger/src/com/android/signflinger/SignedApk.java#56
         */
        const val DEPENDENCY_INFO_BLOCK_ID = 0x504b4453

        /**
         * https://bi-zone.medium.com/easter-egg-in-apk-files-what-is-frosting-f356aa9f4d1
         */
        const val GOOGLE_PLAY_FROSTING_BLOCK_ID = 0x2146444e

        /** https://apt.izzysoft.de/fdroid/index/info#signingblock */
        const val SOURCE_STAMP_V1_BLOCK_ID = 0x2b09189e

        /** https://apt.izzysoft.de/fdroid/index/info#signingblock */
        const val SOURCE_STAMP_V2_BLOCK_ID = 0x6dff800d

        val GOOGLE_BLOCKS: Map<Int, String> = mapOf(
            DEPENDENCY_INFO_BLOCK_ID to "Dependency Info",
            GOOGLE_PLAY_FROSTING_BLOCK_ID to "Google Play Frosting",
            SOURCE_STAMP_V1_BLOCK_ID to "Source Stamp v1",
            SOURCE_STAMP_V2_BLOCK_ID to "Source Stamp v2",
        )

        /**
         * https://gitlab.com/IzzyOnDroid/repo/-/issues/475#note_1729235542
         * https://apt.izzysoft.de/fdroid/index/info#signingblock
         */
        const val APK_CHANNEL_BLOCK_ID = 0x71777777

        val PAYLOAD_BLOCKS: Map<Int, String> = mapOf(
            APK_CHANNEL_BLOCK_ID to "APK Channel",
        )
    }
}
