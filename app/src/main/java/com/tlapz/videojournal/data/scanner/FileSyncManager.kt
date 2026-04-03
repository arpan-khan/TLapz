package com.tlapz.videojournal.data.scanner

import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.tlapz.videojournal.data.database.VideoDao
import com.tlapz.videojournal.data.database.VideoEntry
import com.tlapz.videojournal.domain.model.SyncResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileSyncManager @Inject constructor(
    private val scanner: FileScanner,
    private val dao: VideoDao,
) {
    private val TAG = "FileSyncManager"

    /**
     * Performs a full bidirectional sync:
     * 1. Scans the filesystem
     * 2. Compares with Room cache
     * 3. Inserts added files, removes deleted entries
     */
    suspend fun sync(root: DocumentFile): SyncResult = withContext(Dispatchers.IO) {
        try {
            // Step 1: Scan filesystem
            val scanned = scanner.scan(root)
            val scannedIds = scanned.map { it.id }.toSet()

            // Step 2: Read existing IDs from Room
            val existingIds = dao.getAllIds().toSet()

            // Step 3: Compute diff
            val addedIds = scannedIds - existingIds
            val removedIds = existingIds - scannedIds
            val unchangedCount = (scannedIds intersect existingIds).size

            Log.d(TAG, "Sync: +${addedIds.size} -${removedIds.size} =${unchangedCount}")

            // Step 4: Upsert new entries
            if (addedIds.isNotEmpty()) {
                val toInsert = scanned
                    .filter { it.id in addedIds }
                    .map { sf ->
                        VideoEntry(
                            id = sf.id,
                            filePath = sf.displayName,
                            uriString = sf.uriString,
                            dateTimeMs = sf.dateTimeMs,
                            durationMs = null,
                            note = null,
                            mood = null,
                            fileSizeBytes = sf.fileSizeBytes,
                            isCompressed = false,
                        )
                    }
                dao.upsertAll(toInsert)
            }

            // Step 5: Delete removed entries
            if (removedIds.isNotEmpty()) {
                dao.deleteByIds(removedIds.toList())
            }

            SyncResult.Success(
                added = addedIds.size,
                removed = removedIds.size,
                unchanged = unchangedCount,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed: ${e.message}", e)
            SyncResult.Error(message = e.message ?: "Unknown sync error", cause = e)
        }
    }
}
