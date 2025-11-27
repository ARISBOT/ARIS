/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.database

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.katastima.apkscanner.config.ApkScannerConfig
import org.katastima.apkscanner.database.dao.SigningCertificateDenylistEntity
import org.katastima.apkscanner.database.dao.SigningCertificateDenylistTable
import org.katastima.apkscanner.models.signing.SigningCertificate
import java.io.File

object CertificateDataUtil {

    fun exportCertificateDenylist(database: Database, apkScannerConfig: ApkScannerConfig): List<SigningCertificate> {
        var denylist: MutableList<SigningCertificate> = mutableListOf()

        // Get all deny list entries and store it in the list.
        transaction(database) {
            if (apkScannerConfig.databaseConfig.debug) {
                addLogger(StdOutSqlLogger)
            }

            SigningCertificateDenylistTable
                .selectAll()
                .sortedBy { SigningCertificateDenylistTable.name }
                .map { SigningCertificateDenylistEntity.wrapRow(it) }
                .forEach {
                    val signingCertificate = it.toSigningCertificate()
                    denylist.add(signingCertificate)
                }
        }
        // Sort the deny entry list by name, ignoring case.
        denylist = denylist.sortedBy { it.name.lowercase() }.toMutableList()

        // If there is a template item, move it to the bottom of the list.
        val templateItem = denylist.find { it.name == "TEMPLATE_ENTRY" }
        if (templateItem != null) {
            denylist.remove(templateItem)
            denylist.addLast(templateItem)
        }

        val denylistPath = File(apkScannerConfig.dataConfig.certificateDenylistPath)
        File("${denylistPath.absolutePath}.exported").outputStream().bufferedWriter().use { bufferedWriter ->
            val json = Json {
                encodeDefaults = true
                prettyPrint = true
            }
            bufferedWriter.write(json.encodeToString(denylist))
        }

        return denylist
    }
}
