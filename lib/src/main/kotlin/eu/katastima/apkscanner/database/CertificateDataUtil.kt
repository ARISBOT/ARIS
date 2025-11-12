/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.database

import eu.katastima.apkscanner.config.ApkScannerConfig
import eu.katastima.apkscanner.database.dao.SigningCertificateDenylistEntity
import eu.katastima.apkscanner.database.dao.SigningCertificateDenylistTable
import eu.katastima.apkscanner.models.signing.SigningCertificate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.File

object CertificateDataUtil {

    fun importCertificateData(database: Database, apkScannerConfig: ApkScannerConfig) {
        val json = Json { ignoreUnknownKeys = true }

        importDenylist(json, database, apkScannerConfig)
    }

    private fun importDenylist(json: Json, database: Database, apkScannerConfig: ApkScannerConfig) {
        val denylistPath = File(apkScannerConfig.dataConfig.certificateDenylistPath)
        if (denylistPath.exists()) {
            val denyList: MutableList<SigningCertificate> = mutableListOf()
            val jsonElement = json.parseToJsonElement(denylistPath.readText())
            jsonElement.jsonArray.forEach { denyList.add(json.decodeFromJsonElement<SigningCertificate>(it)) }

            transaction(database) {
                if (apkScannerConfig.databaseConfig.debug) {
                    addLogger(StdOutSqlLogger)
                }

                denyList.sortedBy { it.name }.forEach {
                    SigningCertificateDenylistEntity.new {
                        name = it.name
                        description = it.description
                        sourceUrl = it.sourceUrl
                        dn = it.dn.sorted()
                        sha256 = it.sha256.sorted()
                        sha1 = it.sha1.sorted()
                        md5 = it.md5.sorted()
                    }
                }
            }

            println("Imported ${denyList.size} denied certificates from: ${denylistPath.absolutePath}")
        } else {
            println("Certificate denylist path not specified or does not exist, skipping import")
        }
    }

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
