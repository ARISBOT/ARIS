/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.models.manifest

import kotlinx.serialization.Serializable

@Serializable
data class Manifest(
    val appId: String,
    val versionCode: Int,
    val versionName: String,
    val minSdk: Int,
    val targetSdk: Int,
    val features: List<Feature> = emptyList(),
    val flags: List<Flag> = emptyList(),
    val intentFilters: List<IntentFilter> = emptyList(),
    val permissions: List<Permission> = emptyList(),
    val abis: List<String> = emptyList(),
    val label: String = "",
) {
    companion object {
        val INVALID: Manifest = Manifest(
            appId = "",
            versionCode = 0,
            versionName = "invalid",
            minSdk = 0,
            targetSdk = 0,
        )
    }
}
