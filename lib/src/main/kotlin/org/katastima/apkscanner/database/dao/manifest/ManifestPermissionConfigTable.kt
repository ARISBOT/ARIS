/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.database.dao.manifest

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.katastima.apkscanner.models.manifest.config.ManifestPermissionConfigEntry

object ManifestPermissionConfigTable : IntIdTable("manifest_config_permissions") {
    val name = varchar("name", 255)
    val description = text("description").default("")
    val permissions = array<String>("permissions")
}

class ManifestPermissionConfigEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ManifestPermissionConfigEntity>(ManifestPermissionConfigTable)

    var name by ManifestPermissionConfigTable.name
    var description by ManifestPermissionConfigTable.description
    var permissions by ManifestPermissionConfigTable.permissions

    override fun toString(): String {
        return "ManifestPermissionConfigEntity(" +
                "name=$name, " +
                "description=$description, " +
                "permissions=$permissions" +
                ")"
    }

    fun toManifestPermissionConfigEntry(): ManifestPermissionConfigEntry = ManifestPermissionConfigEntry(
        name = name,
        description = description,
        permissions = permissions.toSet(),
    )
}
