/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.extensions

fun String.containsControlCharacters(): Boolean {
    codePoints().toArray().forEach {
        if (Character.isISOControl(it)) {
            return true
        }
    }
    return false
}
