package com.tlapz.videojournal.domain.usecase

import android.net.Uri
import com.tlapz.videojournal.data.storage.StorageManager
import com.tlapz.videojournal.domain.repository.VideoRepository
import javax.inject.Inject

class DeleteEntryUseCase @Inject constructor(
    private val repository: VideoRepository,
    private val storageManager: StorageManager,
) {
    suspend operator fun invoke(id: String, uriString: String): Boolean {
        return try {
            // Delete the physical file from SAF
            storageManager.deleteFile(uriString)
            // Remove from Room cache
            repository.deleteEntry(id)
            true
        } catch (e: Exception) {
            false
        }
    }
}
