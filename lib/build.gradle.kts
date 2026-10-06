/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

import java.net.URI

plugins {
    id("buildsrc.convention.kotlin-jvm")

    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.shadowGradlePlugin)

    `maven-publish`
}

java {
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    // Apply the kotlinx bundle of dependencies from the version catalog (`gradle/libs.versions.toml`).
    api(libs.bundles.kotlinxEcosystem)

    api(libs.apksig)
    api(libs.apktool.lib)
    api(libs.kaml)
    api(libs.okio)

    api(libs.bundles.exposed)
    api(libs.bundles.logging)

    testImplementation(kotlin("test"))
}

tasks {
    val downloadSampleData = register("downloadSampleData") {
        doLast {
            val sampleDataDir = file("../sampledata")
            val libInfoFile = file("../sampledata/libinfo.jsonl")
            if (!libInfoFile.exists() || libInfoFile.length() == 0L) {
                logger.lifecycle("sampledata directory is empty. Downloading default scanner dataset from Codeberg...")
                sampleDataDir.mkdirs()
                val dataZipUrl = "https://codeberg.org/Katastima/apkscanner-data/archive/main.zip"
                val tempZip = file("${layout.buildDirectory.get().asFile}/tmp/sampledata.zip")
                tempZip.parentFile.mkdirs()

                try {
                    URI.create(dataZipUrl).toURL().openStream().use { input ->
                        tempZip.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    copy {
                        from(zipTree(tempZip)) {
                            eachFile {
                                path = path.substringAfter("/")
                            }
                        }
                        into(sampleDataDir)
                    }
                    logger.lifecycle("Successfully downloaded and unpacked sampledata.")
                } catch (e: Exception) {
                    logger.warn("Could not automatically download sampledata: ${e.message}")
                }
            }
        }
    }

    processResources {
        dependsOn(downloadSampleData)
        from("../sampledata") {
            include("*.json")
            include("*.jsonl")
            into("data")
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "apkscanner"

            from(components["java"])

            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }

            pom {
                name = "ARIS"
                description = "Scan an APK file to check its internals"
                url = "https://katastima.org/apkscanner/overview"
                licenses {
                    license {
                        name = "European Union Public Licence, Version 1.2"
                        url = "https://eupl.eu/"
                    }
                }
                developers {
                    developer {
                        id = "amartinz"
                        name = "Alexander Martinz"
                        email = "alex@katastima.org"
                    }
                }
                scm {
                    connection = "scm:git:https://codeberg.org/Katastima/apkscanner.git"
                    developerConnection = "scm:git:ssh://git@codeberg.org/Katastima/apkscanner.git"
                    url = "https://codeberg.org/Katastima/apkscanner"
                }
            }
        }
    }

    repositories {
        maven {
            url = layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()
        }
    }
}
