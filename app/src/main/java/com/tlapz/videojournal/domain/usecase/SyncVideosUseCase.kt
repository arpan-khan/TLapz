package com.tlapz.videojournal.domain.usecase

import com.tlapz.videojournal.data.scanner.FileSyncManager
import com.tlapz.videojournal.data.storage.StorageManager
import com.tlapz.videojournal.domain.model.SyncResult
import javax.inject.Inject

class SyncVideosUseCase @Inject constructor(
    private val storageManager: StorageManager,
    private val fileSyncManager: FileSyncManager,
) {
    suspend operator fun invoke(): SyncResult {
        val rootDoc = storageManager.getRootDocumentFile()
            ?: return SyncResult.NoFolderSet
        return fileSyncManager.sync(rootDoc)
    }
}
