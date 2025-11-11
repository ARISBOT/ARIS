/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.database

import eu.katastima.apkscanner.config.ApkScannerConfig
import eu.katastima.apkscanner.database.dao.SigningCertificateAllowlistEntity
import eu.katastima.apkscanner.database.dao.SigningCertificateAllowlistTable
import eu.katastima.apkscanner.database.dao.SigningCertificateDenylistEntity
import eu.katastima.apkscanner.database.dao.SigningCertificateDenylistTable
import eu.katastima.apkscanner.models.signing.SigningCertificate
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.File

object CertificateDataUtil {

    fun importCertificateData(database: Database, apkScannerConfig: ApkScannerConfig) {
        val json = Json { ignoreUnknownKeys = true }

        importAllowlist(json, database, apkScannerConfig)
        importDenylist(json, database, apkScannerConfig)
    }

    private fun importAllowlist(json: Json, database: Database, apkScannerConfig: ApkScannerConfig) {
        val allowlistPath = File(apkScannerConfig.dataConfig.certificateAllowlistPath)
        if (allowlistPath.exists()) {
            val allowList: MutableList<SigningCertificate> = mutableListOf()
            allowlistPath.readLines().forEach {
                allowList.add(json.decodeFromString<SigningCertificate>(it))
            }

            transaction(database) {
                if (apkScannerConfig.databaseConfig.debug) {
                    addLogger(StdOutSqlLogger)
                }

                allowList.sortedBy { it.name }.forEach {
                    SigningCertificateAllowlistEntity.new {
                        name = it.name
                        description = it.description
                        dn = it.dn
                        sha256 = it.sha256
                        sha1 = it.sha1
                        md5 = it.md5
                    }
                }
            }

            println("Imported ${allowList.size} allowed certificates from: ${allowlistPath.absolutePath}")
        } else {
            println("Certificate allowlist path not specified or does not exist, skipping import")
        }
    }

    private fun importDenylist(json: Json, database: Database, apkScannerConfig: ApkScannerConfig) {
        val denylistPath = File(apkScannerConfig.dataConfig.certificateDenylistPath)
        if (denylistPath.exists()) {
            val denyList: MutableList<SigningCertificate> = mutableListOf()
            denylistPath.readLines().forEach {
                denyList.add(json.decodeFromString<SigningCertificate>(it))
            }

            transaction(database) {
                if (apkScannerConfig.databaseConfig.debug) {
                    addLogger(StdOutSqlLogger)
                }

                denyList.sortedBy { it.name }.forEach {
                    SigningCertificateDenylistEntity.new {
                        name = it.name
                        description = it.description
                        dn = it.dn
                        sha256 = it.sha256
                        sha1 = it.sha1
                        md5 = it.md5
                    }
                }
            }

            println("Imported ${denyList.size} denied certificates from: ${denylistPath.absolutePath}")
        } else {
            println("Certificate denylist path not specified or does not exist, skipping import")
        }
    }

    fun exportCertificateAllowlist(database: Database, apkScannerConfig: ApkScannerConfig): List<SigningCertificate> {
        val allowlist: MutableList<SigningCertificate> = mutableListOf()

        transaction(database) {
            if (apkScannerConfig.databaseConfig.debug) {
                addLogger(StdOutSqlLogger)
            }

            SigningCertificateAllowlistTable
                .selectAll()
                .sortedBy { SigningCertificateAllowlistTable.name }
                .map { SigningCertificateAllowlistEntity.wrapRow(it) }
                .forEach {
                    val signingCertificate = SigningCertificate(
                        name = it.name,
                        description = it.description,
                        dn = it.dn,
                        sha256 = it.sha256,
                        sha1 = it.sha1,
                        md5 = it.md5,
                    )
                    allowlist.add(signingCertificate)
                }
        }

        val allowlistPath = File(apkScannerConfig.dataConfig.certificateAllowlistPath)
        File("${allowlistPath.absolutePath}.exported").outputStream().bufferedWriter().use { bufferedWriter ->
            val json = Json { encodeDefaults = true }
            allowlist.forEach { entry ->
                bufferedWriter.write(json.encodeToString(entry))
                bufferedWriter.newLine()
            }
        }

        return allowlist
    }

    fun exportCertificateDenylist(database: Database, apkScannerConfig: ApkScannerConfig): List<SigningCertificate> {
        val denylist: MutableList<SigningCertificate> = mutableListOf()

        transaction(database) {
            if (apkScannerConfig.databaseConfig.debug) {
                addLogger(StdOutSqlLogger)
            }

            SigningCertificateDenylistTable
                .selectAll()
                .sortedBy { SigningCertificateDenylistTable.name }
                .map { SigningCertificateDenylistEntity.wrapRow(it) }
                .forEach {
                    val signingCertificate = SigningCertificate(
                        name = it.name,
                        description = it.description,
                        dn = it.dn,
                        sha256 = it.sha256,
                        sha1 = it.sha1,
                        md5 = it.md5,
                    )
                    denylist.add(signingCertificate)
                }
        }

        val denylistPath = File(apkScannerConfig.dataConfig.certificateDenylistPath)
        File("${denylistPath.absolutePath}.exported").outputStream().bufferedWriter().use { bufferedWriter ->
            val json = Json { encodeDefaults = true }
            denylist.forEach { entry ->
                bufferedWriter.write(json.encodeToString(entry))
                bufferedWriter.newLine()
            }
        }

        return denylist
    }
}
