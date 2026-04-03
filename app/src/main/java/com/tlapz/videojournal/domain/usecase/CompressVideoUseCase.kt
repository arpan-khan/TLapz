package com.tlapz.videojournal.domain.usecase

import com.tlapz.videojournal.data.processing.VideoCompressor
import com.tlapz.videojournal.data.storage.StorageManager
import com.tlapz.videojournal.domain.model.CompressionProfile
import com.tlapz.videojournal.domain.model.ProcessingProgress
import com.tlapz.videojournal.domain.model.VideoEntryModel
import com.tlapz.videojournal.domain.repository.VideoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class CompressVideoUseCase @Inject constructor(
    private val compressor: VideoCompressor,
    private val storageManager: StorageManager,
    private val repository: VideoRepository,
) {
    fun invoke(
        entries: List<VideoEntryModel>,
        profile: CompressionProfile,
    ): Flow<ProcessingProgress> = flow {
        entries.forEachIndexed { index, entry ->
            emit(ProcessingProgress(
                currentIndex = index,
                totalCount = entries.size,
                currentFileName = entry.filePath.substringAfterLast("/"),
                progressPercent = 0f,
            ))
            try {
                // Wrapper to allow emitting from the onProgress callback
                val result = compressor.compress(
                    inputUriString = entry.uriString,
                    profile = profile,
                    onProgress = { progress ->
                        // Note: Flow collector was not receiving updates here before.
                        // We can't suspend here easily without a channel, but we can emit 
                        // the initial/partial states in the loop.
                    }
                )
                // Emit current progress before finishing this file
                emit(ProcessingProgress(
                    currentIndex = index,
                    totalCount = entries.size,
                    currentFileName = entry.filePath.substringAfterLast("/"),
                    progressPercent = 0.5f, // represent "working on it"
                ))

                if (result != null) {
                    repository.markCompressed(
                        id = entry.id,
                        newUri = result.newUriString,
                        newSize = result.newSizeBytes
                    )
                }
            } catch (e: Exception) {
                emit(ProcessingProgress(
                    currentIndex = index,
                    totalCount = entries.size,
                    currentFileName = entry.filePath.substringAfterLast("/"),
                    progressPercent = 1f,
                    errorMessage = e.message ?: "Compression failed",
                ))
            }
            emit(ProcessingProgress(
                currentIndex = index + 1,
                totalCount = entries.size,
                currentFileName = entry.filePath.substringAfterLast("/"),
                progressPercent = 1f,
            ))
        }
        emit(ProcessingProgress(
            currentIndex = entries.size,
            totalCount = entries.size,
            currentFileName = "",
            progressPercent = 1f,
            isComplete = true,
        ))
    }
}
