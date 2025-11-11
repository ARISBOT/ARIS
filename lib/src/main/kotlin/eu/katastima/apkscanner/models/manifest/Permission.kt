/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.models.manifest

import kotlinx.serialization.Serializable

@Serializable
data class Permission(
    val name: String,
    val minSdk: Int = -1,
    val maxSdk: Int = -1,
)
