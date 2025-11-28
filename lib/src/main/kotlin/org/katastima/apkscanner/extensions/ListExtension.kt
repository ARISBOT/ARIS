/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.extensions

import kotlin.math.min

fun <T> List<T>.paginate(offset: Int = -1, count: Int = -1): List<T> {
    val fromIndex = if (offset > 0) {
        min(this.size, offset)
    } else {
        0
    }

    val toIndex = if (count > 0) {
        min(this.size, fromIndex + count)
    } else {
        this.size
    }

    return this.subList(fromIndex, toIndex)
}
