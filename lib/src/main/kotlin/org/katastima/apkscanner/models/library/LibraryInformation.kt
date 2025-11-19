/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.models.library

import kotlinx.serialization.Serializable

@Serializable
data class LibraryInformation(
    val libraryId: String,
    val name: String = "",
    val details: String = "",
    val type: String = "",
    val permissions: Array<String> = emptyArray(),
    val url: String = "",
    val modWarningId: String = "",
    val antiFeatures: Array<String> = emptyArray(),
    val license: String = "",
    val emphasize: Int = 0,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LibraryInformation

        if (emphasize != other.emphasize) return false
        if (libraryId != other.libraryId) return false
        if (name != other.name) return false
        if (details != other.details) return false
        if (type != other.type) return false
        if (!permissions.contentEquals(other.permissions)) return false
        if (url != other.url) return false
        if (modWarningId != other.modWarningId) return false
        if (!antiFeatures.contentEquals(other.antiFeatures)) return false
        if (license != other.license) return false

        return true
    }

    override fun hashCode(): Int {
        var result = emphasize
        result = 31 * result + libraryId.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + details.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + permissions.contentHashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + modWarningId.hashCode()
        result = 31 * result + antiFeatures.contentHashCode()
        result = 31 * result + license.hashCode()
        return result
    }
}
