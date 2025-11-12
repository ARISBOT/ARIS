/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.database.dao

import eu.katastima.apkscanner.models.manifest.config.ManifestFlagConfigEntry
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object ManifestFlagConfigTable : IntIdTable("manifest_config_flags") {
    val name = varchar("name", 255)
    val description = text("description").default("")
    val flags = array<String>("flags")
}

class ManifestFlagConfigEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ManifestFlagConfigEntity>(ManifestFlagConfigTable)

    var name by ManifestFlagConfigTable.name
    var description by ManifestFlagConfigTable.description
    var flags by ManifestFlagConfigTable.flags

    override fun toString(): String {
        return "ManifestFlagConfigEntity(" +
                "name=$name, " +
                "description=$description, " +
                "flags=$flags" +
                ")"
    }

    fun toManifestFlagConfigEntry(): ManifestFlagConfigEntry = ManifestFlagConfigEntry(
        name = name,
        description = description,
        flags = flags.toSet(),
    )
}
