/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.database

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.katastima.apkscanner.config.ApkScannerConfig
import org.katastima.apkscanner.data.DataUtil
import org.katastima.apkscanner.database.dao.SigningCertificateDenylistEntity
import org.katastima.apkscanner.database.dao.SigningCertificateDenylistTable
import org.katastima.apkscanner.models.signing.SigningCertificate
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.system.measureTimeMillis

object CertificateDataUtil {

    private val LOGGER = LoggerFactory.getLogger(CertificateDataUtil::class.java)

    fun importCertificateData(database: Database, apkScannerConfig: ApkScannerConfig) {
        val json = Json { ignoreUnknownKeys = true }

        importDenylist(json, database, apkScannerConfig)
    }

    private fun importDenylist(json: Json, database: Database, apkScannerConfig: ApkScannerConfig) {
        val certificateConfigContent = DataUtil.getCertificateConfigContent(apkScannerConfig.dataConfig)
        if (certificateConfigContent.isBlank()) {
            LOGGER.warn("There is no certificate data to import, skipping import")
            return
        }

        val denyList: MutableList<SigningCertificate> = mutableListOf()
        val importDuration = measureTimeMillis {
            val jsonElement = json.parseToJsonElement(certificateConfigContent)
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
        }
        LOGGER.info("Imported {} denied certificates in {} ms", denyList.size, importDuration)
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
