package com.tlapz.videojournal.domain.usecase

import com.tlapz.videojournal.domain.repository.VideoRepository
import javax.inject.Inject

class UpdateEntryUseCase @Inject constructor(
    private val repository: VideoRepository,
) {
    suspend fun updateNote(id: String, note: String?) = repository.updateNote(id, note)
    suspend fun updateMood(id: String, mood: String?) = repository.updateMood(id, mood)
}
