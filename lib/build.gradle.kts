/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

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
    processResources {
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
                name = "APK Scanner"
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
