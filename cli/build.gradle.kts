/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

plugins {
    // Apply the shared build logic from a convention plugin.
    // The shared code is located in `buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts`.
    id("buildsrc.convention.kotlin-jvm")

    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.shadowGradlePlugin)

    application
}

dependencies {
    implementation(project(":lib"))

    implementation(libs.clikt)
}

application {
    mainClass = "org.katastima.apkscanner.cli.AppKt"
}

tasks.named<JavaExec>("run") {
    workingDir = rootDir
}

tasks.shadowJar {
    archiveVersion = ""
}
