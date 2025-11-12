/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.models.manifest

import kotlinx.serialization.Serializable

@Serializable
data class Flag(
    val name: String,
    val value: String,
)
