/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.database.dao.library

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.katastima.apkscanner.models.library.LegacyLibraryInformation
import org.katastima.apkscanner.models.library.LibraryInformation

object LibraryInformationTable : IntIdTable("library_information") {
    val libraryId = varchar("library_id", 255)
    val name = varchar("name", 255).default("")
    val details = text("details").default("")
    val type = varchar("type", 255).default("")
    val permissions = array<String>("permissions")
    val url = varchar("url", 255).default("")
    val modWarningId = varchar("mod_warning_id", 255).default("")
    val antiFeatures = array<String>("anti_features")
    val license = varchar("license", 32).default("")
    val emphasize = integer("emphasize").default(0)
}

class LibraryInformationEntry(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<LibraryInformationEntry>(LibraryInformationTable)

    var libraryId by LibraryInformationTable.libraryId
    var name by LibraryInformationTable.name
    var details by LibraryInformationTable.details
    var type by LibraryInformationTable.type
    var permissions by LibraryInformationTable.permissions
    var url by LibraryInformationTable.url
    var modWarningId by LibraryInformationTable.modWarningId
    var antiFeatures by LibraryInformationTable.antiFeatures
    var license by LibraryInformationTable.license
    var emphasize by LibraryInformationTable.emphasize

    override fun toString(): String {
        return "LibraryInformationEntry(" +
                "id=$id, " +
                "libraryId=$libraryId, " +
                "name=$name, " +
                "details=$details, " +
                "type=$type, " +
                "permissions=$permissions, " +
                "url=$url, " +
                "modWarningId=$modWarningId, " +
                "antiFeatures=$antiFeatures, " +
                "license=$license, " +
                "emphasize=$emphasize" +
                ")"
    }

    fun toLibraryInformation(): LibraryInformation {
        return LibraryInformation(
            libraryId = this.libraryId,
            name = this.name,
            details = this.details,
            type = this.type,
            permissions = this.permissions.toTypedArray(),
            url = this.url,
            modWarningId = this.modWarningId,
            antiFeatures = this.antiFeatures.toTypedArray(),
            license = this.license,
            emphasize = this.emphasize,
        )
    }

    fun toLegacyLibraryInformation(): LegacyLibraryInformation {
        return LegacyLibraryInformation(
            id = this.libraryId,
            emphasize = this.emphasize,
            details = this.details,
            modWarningId = this.modWarningId,
            antiFeatures = this.antiFeatures.toTypedArray(),
            license = this.license,
        )
    }
}
