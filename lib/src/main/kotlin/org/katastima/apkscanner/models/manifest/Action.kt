/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.models.manifest

import kotlinx.serialization.Serializable

@Serializable
data class Action(
    val name: String,
)
