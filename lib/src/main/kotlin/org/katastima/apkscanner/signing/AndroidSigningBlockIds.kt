/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.signing

object AndroidSigningBlockIds {
    /** https://android.googlesource.com/platform/frameworks/base/+/1fb2d90296e30fbb5fd7af1c263deed6b6bff94e/core/java/android/util/apk/ApkSignatureSchemeV2Verifier.java#75 */
    const val APK_SIGNATURE_SCHEME_V2_BLOCK_ID: Int = 0x7109871a

    /** https://android.googlesource.com/platform/frameworks/base/+/1fb2d90296e30fbb5fd7af1c263deed6b6bff94e/core/java/android/util/apk/ApkSignatureSchemeV3Verifier.java#73 */
    const val APK_SIGNATURE_SCHEME_V3_BLOCK_ID: Int = 0xf05368c0.toInt()

    /** https://android.googlesource.com/platform/frameworks/base/+/1fb2d90296e30fbb5fd7af1c263deed6b6bff94e/core/java/android/util/apk/ApkSignatureSchemeV3Verifier.java#74 */
    const val APK_SIGNATURE_SCHEME_V31_BLOCK_ID: Int = 0x1b93ad61

    /** https://android.googlesource.com/platform/tools/apksig/+/9618f8b9ac29126137d89a92d52212ed4984795b/src/main/java/com/android/apksig/internal/apk/ApkSigningBlockUtils.java#100 */
    const val VERITY_PADDING_BLOCK_ID: Int = 0x42726577

    val OK_BLOCKS: Map<Int, String> = mapOf(
        APK_SIGNATURE_SCHEME_V2_BLOCK_ID to "APK Signature Scheme v2",
        APK_SIGNATURE_SCHEME_V3_BLOCK_ID to "APK Signature Scheme v3",
        APK_SIGNATURE_SCHEME_V31_BLOCK_ID to "APK Signature Scheme v3.1",
        VERITY_PADDING_BLOCK_ID to "Verity Padding",
    )

    /**
     * https://developer.android.com/build/dependencies#dependency-info-play
     * https://developer.android.com/build/dependency-verification
     * https://android.googlesource.com/platform/tools/base/+/c71ae138365bcec912656fb39b9cf27fd8be567d/signflinger/src/com/android/signflinger/SignedApk.java#56
     */
    const val DEPENDENCY_INFO_BLOCK_ID: Int = 0x504b4453

    /**
     * https://bi-zone.medium.com/easter-egg-in-apk-files-what-is-frosting-f356aa9f4d1
     */
    const val GOOGLE_PLAY_FROSTING_BLOCK_ID: Int = 0x2146444e

    /** https://apt.izzysoft.de/fdroid/index/info#signingblock */
    const val SOURCE_STAMP_V1_BLOCK_ID: Int = 0x2b09189e

    /** https://apt.izzysoft.de/fdroid/index/info#signingblock */
    const val SOURCE_STAMP_V2_BLOCK_ID: Int = 0x6dff800d

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
    const val APK_CHANNEL_BLOCK_ID: Int = 0x71777777

    val PAYLOAD_BLOCKS: Map<Int, String> = mapOf(
        APK_CHANNEL_BLOCK_ID to "APK Channel",
    )
}
