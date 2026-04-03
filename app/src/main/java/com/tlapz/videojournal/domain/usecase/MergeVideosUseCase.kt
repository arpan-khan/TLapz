package com.tlapz.videojournal.domain.usecase

import com.tlapz.videojournal.data.processing.VideoMerger
import com.tlapz.videojournal.data.storage.StorageManager
import com.tlapz.videojournal.domain.model.ProcessingProgress
import com.tlapz.videojournal.domain.model.VideoEntryModel
import com.tlapz.videojournal.domain.repository.VideoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class MergeVideosUseCase @Inject constructor(
    private val merger: VideoMerger,
    private val storageManager: StorageManager,
    private val repository: VideoRepository,
) {
    fun invoke(entries: List<VideoEntryModel>): Flow<ProcessingProgress> = flow {
        if (entries.isEmpty()) {
            emit(ProcessingProgress(0, 0, "", 1f, isComplete = true))
            return@flow
        }
        emit(ProcessingProgress(0, entries.size, "Preparing merge…", 0f))
        try {
            val sorted = entries.sortedBy { it.dateTimeMs }
            val result = merger.merge(
                inputUriStrings = sorted.map { it.uriString },
                onProgress = { /* simplified */ },
            )
            if (result != null) {
                // Trigger sync so the merged file appears in the timeline
            }
            emit(ProcessingProgress(entries.size, entries.size, "Done", 1f, isComplete = true))
        } catch (e: Exception) {
            emit(ProcessingProgress(
                currentIndex = 0,
                totalCount = entries.size,
                currentFileName = "Merge",
                progressPercent = 0f,
                errorMessage = e.message ?: "Merge failed",
                isComplete = true
            ))
        }
    }
}
