/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.cli

import org.katastima.apkscanner.config.ApkScannerConfig

data class CliConfig(
    var quiet: Boolean = false,
    var apkScannerConfig: ApkScannerConfig = ApkScannerConfig(),
)
