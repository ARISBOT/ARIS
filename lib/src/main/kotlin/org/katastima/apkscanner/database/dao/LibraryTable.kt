/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.database.dao

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object LibraryTable : IntIdTable("libraries") {
    val path = varchar("path", 255)
    var libraryId = varchar("library_id", 255)
    // This does not work with SQLite :/
    //val libraryDefinition = reference("library_definition", LibraryDefinitions)
}

class LibraryEntry(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<LibraryEntry>(LibraryTable)

    var path by LibraryTable.path
    var libraryId by LibraryTable.libraryId
    // This does not work with SQLite :/
    //var libraryDefinition by LibraryDefinition referencedOn Libraries.libraryDefinition

    override fun toString(): String {
        return "LibraryEntry(" +
                "path=$path, " +
                "libraryId=$libraryId" +
                //"libraryDefinition=$libraryDefinition" +
                ")"
    }
}
