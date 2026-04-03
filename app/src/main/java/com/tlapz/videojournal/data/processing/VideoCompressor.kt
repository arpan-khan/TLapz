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
import androidx.media3.transformer.TransformationRequest
import androidx.media3.transformer.Transformer
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.VideoEncoderSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.tlapz.videojournal.data.storage.StorageManager
import com.tlapz.videojournal.domain.model.CompressionProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class VideoCompressor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storageManager: StorageManager,
) {
    private val TAG = "VideoCompressor"

    data class CompressionResult(
        val newUriString: String,
        val newSizeBytes: Long,
    )

    /**
     * Compresses the input video using Media3 Transformer.
     * Writes to cache, then copies back to SAF (replaces original).
     * Returns null on failure.
     */
    suspend fun compress(
        inputUriString: String,
        profile: CompressionProfile,
        onProgress: (Float) -> Unit = {},
    ): CompressionResult? = withContext(Dispatchers.IO) {
        val inputUri = Uri.parse(inputUriString)
        val outputCacheFile = storageManager.createCacheFile("compressed_${System.currentTimeMillis()}.mp4")

        try {
            Log.d(TAG, "Starting compression for: $inputUriString")
            val success = runTransformer(
                context = context,
                inputUri = inputUri,
                outputFile = outputCacheFile,
                profile = profile,
                onProgress = onProgress,
            )
            if (!success) {
                Log.e(TAG, "Transformer aborted or failed.")
                return@withContext null
            }

            // Get TRUE size from the local file before SAF copy
            val newSize = outputCacheFile.length()
            Log.d(TAG, "Transformer finished. Compressed size: $newSize bytes")

            // Find the original SAF DocumentFile and overwrite it
            val originalDoc = storageManager.documentFileFromUriString(inputUriString)
            if (originalDoc == null) {
                Log.e(TAG, "Could not find original SAF document")
                return@withContext null
            }

            // Using pure ContentResolver to ensure "wt" (Write/Truncate) is honored.
            // On some devices DocumentFile.openOutputStream might not truncate.
            val pfd = context.contentResolver.openFileDescriptor(originalDoc.uri, "wt")
            if (pfd == null) {
                Log.e(TAG, "Could not open file descriptor for overwriting")
                return@withContext null
            }

            pfd.use { descriptor ->
                java.io.FileOutputStream(descriptor.fileDescriptor).use { output ->
                    // Explicitly truncate the file to 0 before copying (fixes append bugs on MediaTek)
                    try {
                        output.channel.truncate(0)
                    } catch (e: Exception) {
                        Log.w(TAG, "Channel truncate failed, continuing anyway", e)
                    }
                    
                    outputCacheFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
            }

            Log.d(TAG, "Overwrote original file. Expected size: $newSize bytes")

            CompressionResult(newUriString = inputUriString, newSizeBytes = newSize)
        } catch (e: Exception) {
            Log.e(TAG, "Compression failed: ${e.message}", e)
            null
        } finally {
            outputCacheFile.delete()
        }
    }

    private suspend fun runTransformer(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        profile: CompressionProfile,
        onProgress: (Float) -> Unit,
    ): Boolean = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val mediaItem = MediaItem.fromUri(inputUri)

            // Use Presentation effect to force the desired height
            val videoEffects = listOf(Presentation.createForHeight(profile.height))
            val editedItem = EditedMediaItem.Builder(mediaItem)
                .setEffects(Effects(listOf(), videoEffects))
                .build()

            val videoEncoderSettings = VideoEncoderSettings.Builder()
                .setBitrate(profile.bitrateBps)
                .build()

            val encoderFactory = DefaultEncoderFactory.Builder(context)
                .setRequestedVideoEncoderSettings(videoEncoderSettings)
                .build()

            val transformationRequest = TransformationRequest.Builder()
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setAudioMimeType(MimeTypes.AUDIO_AAC) // Transcode audio for better compatibility
                .setHdrMode(Composition.HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_MEDIACODEC)
                .build()

            var pollingJob: kotlinx.coroutines.Job? = null
            val transformer = Transformer.Builder(context)
                .setTransformationRequest(transformationRequest)
                .setEncoderFactory(encoderFactory)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        pollingJob?.cancel()
                        onProgress(1f)
                        if (continuation.isActive) continuation.resume(true)
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException,
                    ) {
                        pollingJob?.cancel()
                        Log.e(TAG, "Transformer error: ${exportException.message}")
                        if (continuation.isActive) continuation.resume(false)
                    }
                })
                .build()

            // Polling job for progress
            val progressHolder = ProgressHolder()
            pollingJob = launch {
                try {
                    while (isActive) {
                        val state = transformer.getProgress(progressHolder)
                        if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                            onProgress(progressHolder.progress / 100f)
                        } else if (state == Transformer.PROGRESS_STATE_NOT_STARTED || state == Transformer.PROGRESS_STATE_WAITING_FOR_AVAILABILITY) {
                            // ignore
                        } else {
                            break
                        }
                        delay(200)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Polling job error: ${e.message}")
                }
            }

            try {
                transformer.start(editedItem, outputFile.absolutePath)
                continuation.invokeOnCancellation {
                    pollingJob.cancel()
                    // Cancel must also happen on the thread that started it (Main)
                    context.mainExecutor.execute { transformer.cancel() }
                }
            } catch (e: Exception) {
                pollingJob.cancel()
                Log.e(TAG, "Failed to start transformer: ${e.message}")
                if (continuation.isActive) continuation.resume(false)
            }
        }
    }
}
