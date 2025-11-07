/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.models

import kotlinx.serialization.Serializable

@Serializable
data class LegacyLibraryDefinition(
    val id: String,
    val path: String,
    val name: String,
    val type: String,
    val perms: Array<String>,
    val url: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LegacyLibraryDefinition

        if (id != other.id) return false
        if (path != other.path) return false
        if (name != other.name) return false
        if (type != other.type) return false
        if (!perms.contentEquals(other.perms)) return false
        if (url != other.url) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + perms.contentHashCode()
        result = 31 * result + url.hashCode()
        return result
    }
}
