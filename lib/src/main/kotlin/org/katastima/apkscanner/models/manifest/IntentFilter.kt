/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.models.manifest

import kotlinx.serialization.Serializable

/**
 * Represents <intent-filter> within AndroidManifest.xml.
 *
 * Can be contained in:
 * - <activity>
 * - <activity-alias>
 * - <service>
 * - <receiver>
 * - <provider>
 *
 * Must contain:
 * - <action>
 *
 * Can contain:
 * - <category>
 * - <data>
 */
@Serializable
data class IntentFilter(
    // Usually a separate intent filter per action is used, but it is valid to use multiple actions.
    val actions: List<Action>,
    val categories: List<Category> = emptyList(),
    val data: List<Data> = emptyList(),
)
