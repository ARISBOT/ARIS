/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.extensions

fun Boolean.formatYesNo(): String =
    if (this) "Yes" else "No"

fun Boolean.formatVerifiedUnverified(): String =
    if (this) "Verified" else "Unverified"

fun getHexFormat(useUpperCase: Boolean = true): HexFormat = HexFormat {
    upperCase = useUpperCase
    number {
        prefix = "0x"
        minLength = 4
        removeLeadingZeros = true
    }
}

fun Int.formatAsHex(useUpperCase: Boolean = true): String = this.toHexString(getHexFormat(useUpperCase))

fun Long.formatAsHex(useUpperCase: Boolean = true): String = this.toHexString(getHexFormat(useUpperCase))
