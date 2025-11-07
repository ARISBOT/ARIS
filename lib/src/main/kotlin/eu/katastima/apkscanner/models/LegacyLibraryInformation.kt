/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LegacyLibraryInformation(
    val id: String,
    val emphasize: Int,
    val details: String,
    @SerialName("mwid")
    // TODO: check what that means
    val mwid: String,
    @SerialName("anti")
    val antiFeatures: Array<String>,
    // The information jsonl does not contain a license field for all entries.
    val license: String = "",
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LegacyLibraryInformation

        if (emphasize != other.emphasize) return false
        if (id != other.id) return false
        if (details != other.details) return false
        if (mwid != other.mwid) return false
        if (!antiFeatures.contentEquals(other.antiFeatures)) return false
        if (license != other.license) return false

        return true
    }

    override fun hashCode(): Int {
        var result = emphasize
        result = 31 * result + id.hashCode()
        result = 31 * result + details.hashCode()
        result = 31 * result + mwid.hashCode()
        result = 31 * result + antiFeatures.contentHashCode()
        result = 31 * result + license.hashCode()
        return result
    }
}
