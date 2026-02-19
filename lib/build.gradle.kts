/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

import org.jreleaser.model.Active
import org.jreleaser.model.Http
import org.jreleaser.model.Signing.Mode

plugins {
    // Apply the shared build logic from a convention plugin.
    // The shared code is located in `buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts`.
    id("buildsrc.convention.kotlin-jvm")

    // Apply Kotlin Serialization plugin from `gradle/libs.versions.toml`.
    alias(libs.plugins.kotlinPluginSerialization)

    alias(libs.plugins.jreleaser)
    alias(libs.plugins.shadowGradlePlugin)

    `maven-publish`
}

group = "org.katastima.apkscanner"
version = "0.0.8"

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

jreleaser {
    gitRootSearch = true

    project {
        name = "APK Scanner"
        description = "Scan an APK file to check its internals"
    }

    signing {
        active = Active.ALWAYS
        pgp {
            active = Active.ALWAYS
            armored = true
            mode = Mode.COMMAND
            command {
                keyName = findProperty("signing.keyId") as? String
            }
        }
    }

    deploy {
        maven {
            forgejo {
                register("codeberg") {
                    active = Active.ALWAYS
                    url = "https://codeberg.org/api/packages/Katastima/maven"
                    stagingRepository(layout.buildDirectory.dir("staging-deploy").get())

                    artifactOverride {
                        artifactId = "apkscanner"
                    }

                    applyMavenCentralRules = true

                    authorization = Http.Authorization.BEARER
                    password = findProperty("codeberg_access_token") as? String
                }
            }

            mavenCentral {
                register("sonatype") {
                    active = Active.ALWAYS
                    url = "https://central.sonatype.com/api/v1/publisher"
                    stagingRepository(layout.buildDirectory.dir("staging-deploy").get())

                    artifactOverride {
                        artifactId = "apkscanner"
                    }
                }
            }
        }
    }
}
