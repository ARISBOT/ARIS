/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.database.dao

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.katastima.apkscanner.models.library.LegacyLibraryDefinition
import org.katastima.apkscanner.models.library.Library

object LibraryTable : IntIdTable("libraries") {
    val path = varchar("path", 255)
    val libraryInformationEntry = reference("library_definition", LibraryInformationTable)
}

class LibraryEntry(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<LibraryEntry>(LibraryTable)

    var path by LibraryTable.path
    var libraryInformationEntry by LibraryInformationEntry referencedOn LibraryTable.libraryInformationEntry

    override fun toString(): String {
        return "LibraryEntry(" +
                "path=$path, " +
                "libraryDefinition=$libraryInformationEntry" +
                ")"
    }

    fun toLibrary(): Library {
        return Library(
            path = this.path,
            libraryInformation = libraryInformationEntry.toLibraryInformation(),
        )
    }

    fun toLegacyLibraryDefinition(): LegacyLibraryDefinition {
        return LegacyLibraryDefinition(
            id = libraryInformationEntry.libraryId,
            path = this.path,
            name = libraryInformationEntry.name,
            type = libraryInformationEntry.type,
            perms = libraryInformationEntry.permissions.toTypedArray(),
            url = libraryInformationEntry.url,
        )
    }
}
