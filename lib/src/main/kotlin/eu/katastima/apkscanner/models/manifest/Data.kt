/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.models.manifest

import kotlinx.serialization.Serializable

@Serializable
data class Data(
    val scheme: String = "",
    val host: String = "",
    val port: String = "",
    val path: String = "",
    val pathPattern: String = "",
    val pathPrefix: String = "",
    val pathSuffix: String = "",
    val pathAdvancedPattern: String = "",
    val mimeType: String = "",
)
