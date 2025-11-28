/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.data.library

import org.katastima.apkscanner.models.library.LegacyLibraryDefinition
import org.katastima.apkscanner.models.library.LegacyLibraryInformation
import org.katastima.apkscanner.models.library.LibraryInformation

interface LibraryRepository {
    suspend fun getAllInformationEntries(offset: Int = -1, count: Int = -1): List<LegacyLibraryInformation>
    suspend fun countInformationEntries(): Long

    suspend fun getAllDefinitionEntries(offset: Int = -1, count: Int = -1): List<LegacyLibraryDefinition>
    suspend fun countDefinitionEntries(): Long

    suspend fun getLibraryInformationForLibraryPath(libraryPath: String): List<LibraryInformation>
}
