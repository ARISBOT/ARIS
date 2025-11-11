/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.database.dao

import eu.katastima.apkscanner.models.signing.SigningCertificate
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

object SigningCertificateDenylistTable : IntIdTable("signing_certificate_denylist") {
    val name = varchar("name", 255)
    val description = text("description").default("")
    val sourceUrl = varchar("sourceUrl", 255)
    val dn = text("dn").default("")
    val sha256 = varchar("sha256", 64).default("")
    val sha1 = varchar("sha1", 40).default("")
    val md5 = varchar("md5", 32).default("")
}

class SigningCertificateDenylistEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<SigningCertificateDenylistEntity>(SigningCertificateDenylistTable)

    var name by SigningCertificateDenylistTable.name
    var description by SigningCertificateDenylistTable.description
    var sourceUrl by SigningCertificateDenylistTable.sourceUrl
    var dn by SigningCertificateDenylistTable.dn
    var sha256 by SigningCertificateDenylistTable.sha256
    var sha1 by SigningCertificateDenylistTable.sha1
    var md5 by SigningCertificateDenylistTable.md5

    override fun toString(): String {
        return "SigningCertificateDenylistEntity(" +
                "name=$name, " +
                "description=$description, " +
                "sourceUrl=$sourceUrl, " +
                "dn=$dn, " +
                "sha256=$sha256, " +
                "sha1=$sha1, " +
                "md5=$md5" +
                ")"
    }

    fun toSigningCertificate(): SigningCertificate = SigningCertificate(
        name = name,
        description = description,
        sourceUrl = sourceUrl,
        dn = dn,
        sha256 = sha256,
        sha1 = sha1,
        md5 = md5,
    )
}
