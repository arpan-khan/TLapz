package com.tlapz.videojournal.data.scanner

import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.tlapz.videojournal.core.util.Constants
import com.tlapz.videojournal.core.util.DateUtils
import com.tlapz.videojournal.core.util.FileUtils
import com.tlapz.videojournal.domain.model.ScannedFile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileScanner @Inject constructor() {

    private val TAG = "FileScanner"

    /**
     * Scans the given root DocumentFile tree and returns all valid video entries.
     * Gracefully skips files with unrecognized names or read errors.
     */
    fun scan(root: DocumentFile): List<ScannedFile> {
        val results = mutableListOf<ScannedFile>()
        try {
            val mp4Files = FileUtils.collectMp4Files(root)
            for (file in mp4Files) {
                try {
                    val name = file.name ?: continue
                    val parsed = DateUtils.parseFilename(name)
                    if (parsed == null) {
                        Log.d(TAG, "Skipping unrecognized filename: $name")
                        continue
                    }
                    val uriString = file.uri.toString()
                    val id = FileUtils.generateEntryId(uriString)
                    results.add(
                        ScannedFile(
                            id = id,
                            uriString = uriString,
                            displayName = name,
                            dateTimeMs = parsed.epochMs,
                            fileSizeBytes = file.length(),
                            tag = parsed.tag,
                        )
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Error scanning file ${file.name}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during folder scan: ${e.message}", e)
        }
        Log.d(TAG, "Scan complete. Found ${results.size} valid video(s).")
        return results
    }
}
