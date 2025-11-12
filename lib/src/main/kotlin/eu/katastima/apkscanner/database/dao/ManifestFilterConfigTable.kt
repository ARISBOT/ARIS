/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.database.dao

import eu.katastima.apkscanner.models.manifest.config.ManifestFilterConfigEntry
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object ManifestFilterConfigTable : IntIdTable("manifest_config_filters") {
    val name = varchar("name", 255)
    val description = text("description").default("")
    val filters = array<String>("filters")
}

class ManifestFilterConfigEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ManifestFilterConfigEntity>(ManifestFilterConfigTable)

    var name by ManifestFilterConfigTable.name
    var description by ManifestFilterConfigTable.description
    var filters by ManifestFilterConfigTable.filters

    override fun toString(): String {
        return "ManifestFilterConfigEntity(" +
                "name=$name, " +
                "description=$description, " +
                "filters=$filters" +
                ")"
    }

    fun toManifestFilterConfigEntry(): ManifestFilterConfigEntry = ManifestFilterConfigEntry(
        name = name,
        description = description,
        filters = filters.toSet(),
    )
}
