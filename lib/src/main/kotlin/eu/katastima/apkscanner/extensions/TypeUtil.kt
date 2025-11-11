/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.extensions

fun Boolean.formatYesNo(): String =
    if (this) "Yes" else "No"

fun Boolean.formatValidInvalid(): String =
    if (this) "Valid" else "Invalid"

fun Int.formatAsHex(upperCase: Boolean = true): String =
    "0x${this.toHexString(if (upperCase) HexFormat.UpperCase else HexFormat.Default)}"
