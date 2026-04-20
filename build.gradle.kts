/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

tasks.withType<Wrapper> {
    // https://docs.gradle.org/current/release-notes.html
    // https://github.com/gradle/gradle/releases
    gradleVersion = "9.4.1"
    distributionType = Wrapper.DistributionType.ALL
    validateDistributionUrl = true
}
