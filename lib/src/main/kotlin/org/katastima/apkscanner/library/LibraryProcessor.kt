/*
 * SPDX-FileCopyrightText: Katastima Authors
 * SPDX-License-Identifier: EUPL-1.2
 */

package org.katastima.apkscanner.library

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.katastima.apkscanner.data.library.LibraryRepository
import org.katastima.apkscanner.models.library.LibraryCheckResult
import org.katastima.apkscanner.models.library.LibraryInformation
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.math.max
import kotlin.system.measureTimeMillis

class LibraryProcessor(
    private val libraryRepository: LibraryRepository,
    private val backgroundDispatcher: CoroutineDispatcher,
    private val ioDispatcher: CoroutineDispatcher,
) {

    suspend fun process(outputDir: File): LibraryCheckResult = withContext(ioDispatcher) {
        val libraryInformationSet = detectLibraries(outputDir)

        val detectedLibraries = libraryInformationSet.sortedBy { it.name.lowercase() }
        val offendingLibraries = detectedLibraries.filter { it.antiFeatures.isNotEmpty() }

        val antiFeatures = detectAntiFeatures(offendingLibraries)
        val modWarningIds = detectModWarningIds(offendingLibraries)

        return@withContext LibraryCheckResult(
            detectedLibraries = detectedLibraries,
            offendingLibraries = offendingLibraries,
            antiFeatures = antiFeatures,
            modWarningIds = modWarningIds,
        )
    }

    private suspend fun detectLibraries(outputDir: File): Set<LibraryInformation> {
        val libraryInformationSet = mutableSetOf<LibraryInformation>()

        var totalProcessingDuration = 0L
        var totalProcessedDirectories = 0L
        outputDir
            // Filter by directories, where the name contains "smali".
            .listFiles { it.isDirectory && it.name.lowercase().contains("smali") }
            .forEach { smaliDirectory ->
                var processedDirectories = 0
                val processSmaliDirectoryDuration = measureTimeMillis {
                    processSmaliDirectory(smaliDirectory) {
                        libraryInformationSet.addAll(it)
                        processedDirectories++
                    }
                }
                totalProcessedDirectories += processedDirectories
                totalProcessingDuration += processSmaliDirectoryDuration
                LOGGER.debug("processSmaliDirectory(${smaliDirectory.name}): {} ms for {} directories", processSmaliDirectoryDuration, processedDirectories)
            }
        LOGGER.debug(
            "scanForLibraries(): processed a total of {} directories in {} ms ({} ms / directory)",
            totalProcessedDirectories, totalProcessingDuration, totalProcessingDuration / max(1, totalProcessedDirectories)
        )

        return libraryInformationSet
    }

    private suspend fun processSmaliDirectory(
        smaliDirectory: File,
        onProcess: (informationSet: Set<LibraryInformation>) -> Unit,
    ) = withContext(ioDispatcher) {
        val smaliDirectoryPath = "${smaliDirectory.absolutePath}${File.separator}"

        // Walk through all the directories within the smali directory
        smaliDirectory
            .walkTopDown()
            .filter { it.isDirectory }
            .filter { it.absolutePath != smaliDirectory.absolutePath }
            .toSet()
            .map { async { processSmaliChildDirectory(it.absolutePath, smaliDirectoryPath) } }
            .awaitAll()
            .forEach { onProcess(it) }
    }

    private suspend fun processSmaliChildDirectory(
        absoluteDirectoryPath: String,
        absoluteSmaliDirectoryPath: String,
    ): Set<LibraryInformation> = withContext(backgroundDispatcher) {
        val libraryId = absoluteDirectoryPath.replace(absoluteSmaliDirectoryPath, "")
        val libraryPath = "${File.separator}${libraryId}"
        return@withContext libraryRepository.getLibraryInformationForLibraryPath(libraryPath).toSet()
    }

    private suspend fun detectAntiFeatures(offendingLibraries: List<LibraryInformation>): Set<String> = withContext(backgroundDispatcher) {
        val antiFeatureSet = sortedSetOf<String>()
        offendingLibraries
            .map { it.antiFeatures }
            .forEach { antiFeatures ->
                antiFeatures
                    .filter { it.isNotBlank() }
                    .forEach { antiFeatureSet.add(it) }
            }
        return@withContext antiFeatureSet
    }

    private suspend fun detectModWarningIds(offendingLibraries: List<LibraryInformation>): Set<String> = withContext(backgroundDispatcher) {
        val modWarningIdSet = sortedSetOf<String>()
        offendingLibraries
            .map { it.modWarningId }
            .filter { it.isNotBlank() }
            .forEach { modWarningIdSet.add(it) }
        return@withContext modWarningIdSet
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(LibraryProcessor::class.java)
    }
}
