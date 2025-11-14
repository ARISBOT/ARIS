/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.models.signing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class SigningBlockResult(
    @SerialName("blocks") val blocksFormatted: Map<String, String> = emptyMap(),
    @SerialName("unknownBlocks") val unknownBlocksFormatted: List<String> = emptyList(),
    @Transient val blocks: Set<Int> = emptySet(),
    @Transient val unknownBlocks: Set<Int> = emptySet(),
)
