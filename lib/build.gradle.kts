/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

plugins {
    // Apply the shared build logic from a convention plugin.
    // The shared code is located in `buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts`.
    id("buildsrc.convention.kotlin-jvm")

    // Apply Kotlin Serialization plugin from `gradle/libs.versions.toml`.
    alias(libs.plugins.kotlinPluginSerialization)

    alias(libs.plugins.shadowGradlePlugin)

    `maven-publish`
    signing
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
            groupId = "org.katastima"
            artifactId = "apkscanner"
            version = "0.0.4"

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
                description = "do not use yet - work in progress"
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
                        email = "alex@amartinz.at"
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
            name = "Codeberg"
            url = uri("https://codeberg.org/api/packages/Katastima/maven")

            credentials(HttpHeaderCredentials::class) {
                name = "Authorization"
                // Get access token from e.g.: ~/.gradle/gradle.properties.
                // Create a new token at https://codeberg.org/user/settings/applications, granting "package" -> "Read and write".
                value = "token ${findProperty("codeberg_access_token") as? String}"
            }

            authentication {
                register("header", HttpHeaderAuthentication::class)
            }
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications["maven"])
}
