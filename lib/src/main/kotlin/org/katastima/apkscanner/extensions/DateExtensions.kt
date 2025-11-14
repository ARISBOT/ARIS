/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.extensions

import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import java.util.*
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
fun Date.toLocalDateTime(): LocalDateTime =
    Instant.fromEpochMilliseconds(this.time).toLocalDateTime(
        FixedOffsetTimeZone(
            UtcOffset.ZERO
        )
    )

@OptIn(ExperimentalTime::class)
fun nowAsLocalDate(): LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC)
