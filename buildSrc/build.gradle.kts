/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

plugins {
    // The Kotlin DSL plugin provides a convenient way to develop convention plugins.
    // Convention plugins are located in `src/main/kotlin`, with the file extension `.gradle.kts`,
    // and are applied in the project's `build.gradle.kts` files as required.
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Add a dependency on the Kotlin Gradle plugin, so that convention plugins can apply it.
    implementation(libs.kotlinGradlePlugin)
}

tasks.withType<Wrapper> {
    // https://docs.gradle.org/current/release-notes.html
    // https://github.com/gradle/gradle/releases
    gradleVersion = "9.2.0"
    distributionType = Wrapper.DistributionType.ALL
    validateDistributionUrl = true
}
