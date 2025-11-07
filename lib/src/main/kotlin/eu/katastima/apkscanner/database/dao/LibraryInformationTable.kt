/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.database.dao

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object LibraryInformationTable : IntIdTable("library_information") {
    val libraryId = varchar("library_id", 255)
    val name = varchar("name", 255).default("")
    val details = text("details").default("")
    val type = varchar("type", 255).default("")
    val permissions = array<String>("permissions")
    val url = varchar("url", 255).default("")
    val mwid = varchar("mwid", 255).default("")
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
    var mwid by LibraryInformationTable.mwid
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
                "mwid=$mwid, " +
                "antiFeatures=$antiFeatures, " +
                "license=$license, " +
                "emphasize=$emphasize" +
                ")"
    }
}
