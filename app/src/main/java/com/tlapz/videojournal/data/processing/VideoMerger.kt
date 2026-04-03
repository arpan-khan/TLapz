package com.tlapz.videojournal.data.processing

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.tlapz.videojournal.core.util.Constants
import com.tlapz.videojournal.core.util.DateUtils
import com.tlapz.videojournal.data.storage.StorageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class VideoMerger @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storageManager: StorageManager,
) {
    private val TAG = "VideoMerger"

    data class MergeResult(
        val outputUriString: String,
        val outputSizeBytes: Long,
    )

    /**
     * Merges multiple videos into one, sorted by their input order.
     * Outputs to the root/YYYY/MM/ folder using the timestamp of the first video.
     * Returns null on failure.
     */
    suspend fun merge(
        inputUriStrings: List<String>,
        onProgress: (Float) -> Unit = {},
    ): MergeResult? = withContext(Dispatchers.IO) {
        if (inputUriStrings.isEmpty()) return@withContext null

        val outputCacheFile = storageManager.createCacheFile("merged_${System.currentTimeMillis()}.mp4")

        try {
            val editedItems = inputUriStrings.map { uriString ->
                EditedMediaItem.Builder(MediaItem.fromUri(Uri.parse(uriString))).build()
            }
            val sequence = EditedMediaItemSequence(editedItems)
            val composition = Composition.Builder(listOf(sequence)).build()

            val success = runTransformerMerge(context, composition, outputCacheFile)
            if (!success) return@withContext null

            // Save merged file to root/YYYY/MM/merged_YYYY-MM-DD_HH-MM-SS.mp4
            val nowMs = System.currentTimeMillis()
            val (year, month) = DateUtils.getFolderParts(nowMs)
            val root = storageManager.getRootDocumentFile() ?: return@withContext null
            val yearDir = root.findFile(year) ?: root.createDirectory(year) ?: return@withContext null
            val monthDir = yearDir.findFile(month) ?: yearDir.createDirectory(month) ?: return@withContext null
            val mergedName = "${Constants.MERGED_PREFIX}${DateUtils.generateFilenameStem()}"
            val outputDoc = monthDir.createFile("video/mp4", mergedName) ?: return@withContext null

            storageManager.openOutputStream(outputDoc.uri)?.use { out ->
                outputCacheFile.inputStream().use { it.copyTo(out) }
            }

            Log.d(TAG, "Merge complete → ${outputDoc.uri}")
            MergeResult(
                outputUriString = outputDoc.uri.toString(),
                outputSizeBytes = outputDoc.length(),
            )
        } catch (e: Exception) {
            Log.e(TAG, "Merge failed: ${e.message}", e)
            null
        } finally {
            outputCacheFile.delete()
        }
    }

    private suspend fun runTransformerMerge(
        context: Context,
        composition: Composition,
        outputFile: File,
    ): Boolean = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val transformer = Transformer.Builder(context)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        if (continuation.isActive) continuation.resume(true)
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException,
                    ) {
                        Log.e(TAG, "Merge transformer error: ${exportException.message}")
                        if (continuation.isActive) continuation.resume(false)
                    }
                })
                .build()

            try {
                transformer.start(composition, outputFile.absolutePath)
                continuation.invokeOnCancellation {
                    context.mainExecutor.execute { transformer.cancel() }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start merge transformer: ${e.message}")
                if (continuation.isActive) continuation.resume(false)
            }
        }
    }
}
