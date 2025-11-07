/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package eu.katastima.apkscanner.utils

import kotlin.random.Random

object Randomizer {
    private val CHAR_POOL: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

    fun getRandomString(length: Int = 8) = (1..length)
        .map { Random.nextInt(0, CHAR_POOL.size).let { CHAR_POOL[it] } }
        .joinToString("")
}
