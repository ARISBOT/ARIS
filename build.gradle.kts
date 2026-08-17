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
