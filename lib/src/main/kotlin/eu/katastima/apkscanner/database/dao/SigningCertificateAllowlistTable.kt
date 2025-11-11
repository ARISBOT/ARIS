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

object SigningCertificateAllowlistTable : IntIdTable("signing_certificate_allowlist") {
    val name = varchar("name", 255)
    val description = text("description").default("")
    val dn = text("dn").default("")
    val sha256 = varchar("sha256", 64).default("")
    val sha1 = varchar("sha1", 40).default("")
    val md5 = varchar("md5", 32).default("")
}

class SigningCertificateAllowlistEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<SigningCertificateAllowlistEntity>(SigningCertificateAllowlistTable)

    var name by SigningCertificateAllowlistTable.name
    var description by SigningCertificateAllowlistTable.description
    var dn by SigningCertificateAllowlistTable.dn
    var sha256 by SigningCertificateAllowlistTable.sha256
    var sha1 by SigningCertificateAllowlistTable.sha1
    var md5 by SigningCertificateAllowlistTable.md5

    override fun toString(): String {
        return "SigningCertificateAllowlistEntity(" +
                "name=$name, " +
                "description=$description, " +
                "dn=$dn, " +
                "sha256=$sha256, " +
                "sha1=$sha1, " +
                "md5=$md5" +
                ")"
    }

    fun toSigningCertificate(): SigningCertificate = SigningCertificate(
        name = name,
        description = description,
        dn = dn,
        sha256 = sha256,
        sha1 = sha1,
        md5 = md5,
    )
}
