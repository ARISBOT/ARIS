/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

import org.jreleaser.model.Active
import org.jreleaser.model.Signing.Mode

plugins {
    id("base")

    alias(libs.plugins.jreleaser)
}

tasks.withType<Wrapper> {
    // https://docs.gradle.org/current/release-notes.html
    // https://github.com/gradle/gradle/releases
    gradleVersion = "9.7.0"
    distributionType = Wrapper.DistributionType.BIN
    validateDistributionUrl = true
}

jreleaser {
    project {
        name = "ARIS"
        description = "Scan an APK file to check its internals"
    }

    files {
        artifact {
            path = project(":cli").layout.buildDirectory.dir("libs").get().file("cli-all.jar")
            transform = "apk-scanner_v$version.jar"
        }
    }

    release {
        codeberg {
            enabled = false
        }

        forgejo {
            enabled = true

            host = "codeberg.org"
            apiEndpoint = "https://codeberg.org"

            username = findProperty("forgejo.username") as? String
            token = findProperty("forgejo.token") as? String

            sign = true
            skipTag = false
            skipRelease = false

            prerelease {
                enabled = true
            }

            commitAuthor {
                name = findProperty("forgejo.commitAuthor.name") as? String
                email = findProperty("forgejo.commitAuthor.email") as? String
            }

            changelog {
                links = true

                formatted = Active.ALWAYS
                preset = "conventional-commits"

                contributors {
                    enabled = false
                }
            }
        }
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
            mavenCentral {
                register("sonatype-apkscanner") {
                    active = Active.ALWAYS
                    url = "https://central.sonatype.com/api/v1/publisher"
                    stagingRepository(project(":lib").layout.buildDirectory.dir("staging-deploy").get())

                    artifactOverride {
                        artifactId = "apkscanner"
                    }
                }
            }
        }
    }
}
